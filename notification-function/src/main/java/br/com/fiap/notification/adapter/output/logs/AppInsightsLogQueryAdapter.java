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

@ApplicationScoped
public class AppInsightsLogQueryAdapter implements LogQueryPort {

    private static final Logger log = LoggerFactory.getLogger(AppInsightsLogQueryAdapter.class);

    private static final String QUERY_URL = "https://api.applicationinsights.io/v1/apps/%s/query";
    private static final DateTimeFormatter KQL_TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final int MAX_ROWS = 50;

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
            throw new LogQueryException("Application Insights query API returned %d: %s"
                    .formatted(response.statusCode(), response.body()));
        }

        return parse(response.body());
    }

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
