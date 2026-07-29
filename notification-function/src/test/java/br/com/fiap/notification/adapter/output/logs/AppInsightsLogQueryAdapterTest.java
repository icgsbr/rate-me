package br.com.fiap.notification.adapter.output.logs;

import br.com.fiap.notification.domain.ServiceErrorLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppInsightsLogQueryAdapterTest {

    private static final String APP_ID = "app-123";
    private static final String API_KEY = "secret-key";
    private static final String ROLE_NAME = "feedback-service";

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 7, 28, 9, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Captor
    private ArgumentCaptor<HttpRequest> requestCaptor;

    private AppInsightsLogQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = adapterWith(APP_ID, API_KEY);
    }

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("refuses to query when the workspace id is not configured")
    void refusesWithoutAppId() {
        assertThatThrownBy(() -> adapterWith("", API_KEY).findErrorsBetween(FROM, TO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.log-scan.insights-app-id");
    }

    @Test
    @DisplayName("refuses to query when the api key is not configured")
    void refusesWithoutApiKey() {
        assertThatThrownBy(() -> adapterWith(APP_ID, "  ").findErrorsBetween(FROM, TO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.log-scan.insights-api-key");
    }

    @Test
    @DisplayName("maps a successful query response onto domain error records")
    void mapsSuccessfulResponse() throws Exception {
        stubResponse(200, """
                {"tables":[{"name":"PrimaryResult",
                  "columns":[{"name":"timestamp"},{"name":"errorKind"},{"name":"detail"},{"name":"operation_Id"}],
                  "rows":[
                    ["2026-07-28T09:10:00Z","trace","could not send e-mail","op-1"],
                    ["2026-07-28T09:20:00Z","exception","IllegalStateException :: boom","op-2"]
                  ]}]}""");

        List<ServiceErrorLog> errors = adapter.findErrorsBetween(FROM, TO);

        assertThat(errors).hasSize(2);
        assertThat(errors.get(0).timestamp())
                .isEqualTo(OffsetDateTime.of(2026, 7, 28, 9, 10, 0, 0, ZoneOffset.UTC));
        assertThat(errors.get(0).kind()).isEqualTo("trace");
        assertThat(errors.get(0).detail()).isEqualTo("could not send e-mail");
        assertThat(errors.get(0).operationId()).isEqualTo("op-1");
        assertThat(errors.get(1).kind()).isEqualTo("exception");
    }

    @Test
    @DisplayName("sends the api key and a role-scoped KQL query to the right workspace")
    void sendsAWellFormedRequest() throws Exception {
        stubResponse(200, "{\"tables\":[]}");

        adapter.findErrorsBetween(FROM, TO);

        verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest request = requestCaptor.getValue();

        assertThat(request.uri())
                .hasToString("https://api.applicationinsights.io/v1/apps/app-123/query");
        assertThat(request.headers().firstValue("X-Api-Key")).contains(API_KEY);
        assertThat(request.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(request.timeout()).contains(Duration.ofSeconds(30));
        assertThat(request.method()).isEqualTo("POST");
    }

    @Test
    @DisplayName("returns an empty list when the query matches nothing")
    void returnsEmptyListForAnEmptyResult() throws Exception {
        stubResponse(200, """
                {"tables":[{"columns":[{"name":"timestamp"}],"rows":[]}]}""");

        assertThat(adapter.findErrorsBetween(FROM, TO)).isEmpty();
    }

    @Test
    @DisplayName("substitutes an empty string for columns the query did not return")
    void toleratesMissingColumns() throws Exception {
        stubResponse(200, """
                {"tables":[{
                  "columns":[{"name":"timestamp"},{"name":"detail"}],
                  "rows":[["2026-07-28T09:10:00Z","only a detail"]]}]}""");

        List<ServiceErrorLog> errors = adapter.findErrorsBetween(FROM, TO);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).detail()).isEqualTo("only a detail");
        assertThat(errors.get(0).kind()).isEmpty();
        assertThat(errors.get(0).operationId()).isEmpty();
    }

    @Test
    @DisplayName("reports a non-200 answer with the status and the body")
    void reportsAnUnsuccessfulStatus() throws Exception {
        stubResponse(403, "{\"error\":{\"code\":\"Forbidden\"}}");

        assertThatThrownBy(() -> adapter.findErrorsBetween(FROM, TO))
                .isInstanceOf(LogQueryException.class)
                .hasMessage("Application Insights query API returned 403: {\"error\":{\"code\":\"Forbidden\"}}");
    }

    @Test
    @DisplayName("reports an unparseable payload rather than returning half a result")
    void reportsAnUnparseablePayload() throws Exception {
        stubResponse(200, """
                {"tables":[{"columns":[{"name":"timestamp"}],"rows":[["not-a-timestamp"]]}]}""");

        assertThatThrownBy(() -> adapter.findErrorsBetween(FROM, TO))
                .isInstanceOf(LogQueryException.class)
                .hasMessage("could not parse the Application Insights response");
    }

    @Test
    @DisplayName("wraps a transport failure in a LogQueryException")
    void wrapsTransportFailure() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("connection reset"));

        assertThatThrownBy(() -> adapter.findErrorsBetween(FROM, TO))
                .isInstanceOf(LogQueryException.class)
                .hasMessage("could not reach the Application Insights query API")
                .hasRootCauseMessage("connection reset");
    }

    @Test
    @DisplayName("restores the interrupt flag when the query is interrupted")
    void restoresInterruptFlag() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("shutting down"));

        assertThatThrownBy(() -> adapter.findErrorsBetween(FROM, TO))
                .isInstanceOf(LogQueryException.class)
                .hasMessage("interrupted while querying Application Insights");

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    private AppInsightsLogQueryAdapter adapterWith(String appId, String apiKey) {
        return new AppInsightsLogQueryAdapter(httpClient, new ObjectMapper(), appId, apiKey, ROLE_NAME);
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(int status, String body) throws Exception {
        when(httpResponse.statusCode()).thenReturn(status);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
    }
}
