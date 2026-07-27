package br.com.fiap.notification.adapter.output.logs;

import br.com.fiap.notification.application.port.output.LogQueryPort;
import br.com.fiap.notification.domain.ServiceErrorLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the monitored service's error logs from the Application Insights query API.
 *
 * <p>Authentication is an Application Insights <em>API key</em> ({@code X-Api-Key}) rather
 * than a managed identity, because granting a role assignment needs Owner on the
 * subscription and the project only has Contributor on the resource group.</p>
 *
 * <p>Uses the JDK HTTP client on purpose: one small POST does not justify pulling a REST
 * client extension into a function that has to cold-start.</p>
 */
@ApplicationScoped
public class AppInsightsLogQueryAdapter implements LogQueryPort {

    private static final Logger log = LoggerFactory.getLogger(AppInsightsLogQueryAdapter.class);

    private static final String QUERY_URL = "https://api.applicationinsights.io/v1/apps/%s/query";
    private static final DateTimeFormatter KQL_TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** Ceiling on how many records one alert e-mail reports on. */
    private static final int MAX_ROWS = 50;

    /**
     * {@code traces} carries the ERROR-level log statements (severityLevel 3 = ERROR,
     * 4 = CRITICAL); {@code exceptions} carries what escaped without ever being logged,
     * which is how an unmapped 500 shows up. Both are needed to see every error.
     *
     * <p>The column is {@code errorKind}, not {@code kind}: {@code kind} is a KQL keyword
     * and the query is rejected with a syntax error.</p>
     */
    private static final String KQL = """
            union
              (traces     | where severityLevel >= 3 | extend errorKind = "trace",     detail = message),
              (exceptions |                            extend errorKind = "exception", detail = strcat(type, " :: ", outerMessage))
            | where cloud_RoleName == "%s"
            | where timestamp > todatetime("%s") and timestamp <= todatetime("%s")
            | project timestamp, errorKind, detail, operation_Id
            | order by timestamp asc
            | take %d
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String appId;
    private final String apiKey;
    private final String roleName;

    public AppInsightsLogQueryAdapter(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "app.log-scan.insights-app-id") String appId,
            @ConfigProperty(name = "app.log-scan.insights-api-key") String apiKey,
            @ConfigProperty(name = "app.log-scan.role-name") String roleName) {
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.apiKey = apiKey;
        this.roleName = roleName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public List<ServiceErrorLog> findErrorsBetween(OffsetDateTime from, OffsetDateTime to) {
        if (appId.isBlank() || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "app.log-scan.insights-app-id and app.log-scan.insights-api-key must be configured");
        }

        String kql = KQL.formatted(roleName, KQL_TIMESTAMP.format(from), KQL_TIMESTAMP.format(to), MAX_ROWS);

        HttpResponse<String> response;
        try {
            ObjectNode payload = objectMapper.createObjectNode().put("query", kql);
            HttpRequest request = HttpRequest.newBuilder(URI.create(QUERY_URL.formatted(appId)))
                    .header("Content-Type", "application/json")
                    .header("X-Api-Key", apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LogQueryException("interrupted while querying Application Insights", e);
        } catch (Exception e) {
            throw new LogQueryException("could not reach the Application Insights query API", e);
        }

        if (response.statusCode() != 200) {
            // The body carries the reason (bad key, unknown app id, malformed KQL); it is
            // small and contains no telemetry, so it is safe to log.
            throw new LogQueryException("Application Insights query API returned %d: %s"
                    .formatted(response.statusCode(), response.body()));
        }

        return parse(response.body());
    }

    /**
     * The API answers with {@code tables[].columns[]} + {@code tables[].rows[]}, i.e. rows are
     * positional arrays. Column positions are resolved by name so a change in the projection
     * cannot silently shift the fields.
     */
    private List<ServiceErrorLog> parse(String body) {
        List<ServiceErrorLog> errors = new ArrayList<>();
        try {
            JsonNode table = objectMapper.readTree(body).path("tables").path(0);

            Map<String, Integer> columnIndex = new HashMap<>();
            JsonNode columns = table.path("columns");
            for (int i = 0; i < columns.size(); i++) {
                columnIndex.put(columns.get(i).path("name").asText(), i);
            }

            for (JsonNode row : table.path("rows")) {
                errors.add(new ServiceErrorLog(
                        OffsetDateTime.parse(text(row, columnIndex, "timestamp")),
                        text(row, columnIndex, "errorKind"),
                        text(row, columnIndex, "detail"),
                        text(row, columnIndex, "operation_Id")));
            }
        } catch (Exception e) {
            throw new LogQueryException("could not parse the Application Insights response", e);
        }

        log.debug("Application Insights returned {} error record(s) for role {}", errors.size(), roleName);
        return errors;
    }

    private static String text(JsonNode row, Map<String, Integer> columnIndex, String column) {
        Integer index = columnIndex.get(column);
        return index == null ? "" : row.path(index).asText("");
    }
}
