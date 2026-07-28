package br.com.fiap.notification.adapter.input.function;

import br.com.fiap.notification.application.port.input.SendCriticalNotificationUseCase;
import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.Urgencia;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CriticalNotificationFunctionTest {

    @Mock
    private SendCriticalNotificationUseCase sendNotification;

    @Captor
    private ArgumentCaptor<CriticalNotification> notificationCaptor;

    private CriticalNotificationFunction function;

    @BeforeEach
    void setUp() {
        function = new CriticalNotificationFunction();
        function.sendNotification = sendNotification;
        function.objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("accepts a valid payload, dispatches it and answers 200 SENT")
    void acceptsValidPayload() {
        String payload = """
                {"descricao":"database unreachable","urgencia":"critica","dataEnvio":"2026-07-28T10:00:00Z"}""";

        HttpResponseMessage response = function.run(FakeHttpRequestMessage.withBody(payload), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(response.getBody().toString())
                .contains("\"status\":\"SENT\"")
                .contains("\"urgencia\":\"CRITICA\"")
                .contains("2026-07-28T10:00");

        verify(sendNotification).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().descricao()).isEqualTo("database unreachable");
        assertThat(notificationCaptor.getValue().urgencia()).isEqualTo(Urgencia.CRITICA);
    }

    @Test
    @DisplayName("ignores unknown JSON properties and defaults a missing dataEnvio")
    void toleratesUnknownPropertiesAndMissingDate() {
        String payload = """
                {"descricao":"disk full","urgencia":"ALTA","origem":"feedback-service"}""";

        HttpResponseMessage response = function.run(FakeHttpRequestMessage.withBody(payload), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        verify(sendNotification).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().dataEnvio()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejects a blank body with 400 invalid_payload")
    void rejectsBlankBody(String body) {
        HttpResponseMessage response = function.run(FakeHttpRequestMessage.withBody(body), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString())
                .contains("\"error\":\"invalid_payload\"")
                .contains("a JSON body is required");
        verifyNoInteractions(sendNotification);
    }

    @Test
    @DisplayName("rejects an absent body with 400 invalid_payload")
    void rejectsAbsentBody() {
        HttpResponseMessage response = function.run(FakeHttpRequestMessage.withoutBody(), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("a JSON body is required");
        verifyNoInteractions(sendNotification);
    }

    @Test
    @DisplayName("surfaces a domain validation error as 400 with the domain message")
    void rejectsInvalidDomainValue() {
        String payload = """
                {"descricao":"boom","urgencia":"URGENTISSIMO"}""";

        HttpResponseMessage response = function.run(FakeHttpRequestMessage.withBody(payload), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString())
                .contains("\"error\":\"invalid_payload\"")
                .contains("urgencia must be one of BAIXA, MEDIA, ALTA, CRITICA");
        verifyNoInteractions(sendNotification);
    }

    @Test
    @DisplayName("reports malformed JSON as 400 with a distinct message")
    void rejectsMalformedJson() {
        HttpResponseMessage response = function.run(FakeHttpRequestMessage.withBody("{not json"), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("malformed JSON body: ");
        verifyNoInteractions(sendNotification);
    }

    @Test
    @DisplayName("answers 500 delivery_failed when dispatching the alert blows up")
    void reportsDeliveryFailure() {
        doThrow(new IllegalStateException("smtp refused the connection"))
                .when(sendNotification).send(any(CriticalNotification.class));

        String payload = """
                {"descricao":"boom","urgencia":"ALTA"}""";

        HttpResponseMessage response = function.run(FakeHttpRequestMessage.withBody(payload), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().toString())
                .contains("\"error\":\"delivery_failed\"")
                .contains("smtp refused the connection");
    }

    @Test
    @DisplayName("falls back to a raw error body when the response cannot be serialised")
    void fallsBackWhenSerialisationFails() {
        // Subclassing beats spying here: reading the request still needs a fully working
        // ObjectMapper, only the response serialisation must fail.
        function.objectMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) {
                throw new IllegalStateException("no serializer for " + value);
            }
        };

        String payload = """
                {"descricao":"boom","urgencia":"ALTA"}""";

        HttpResponseMessage response = function.run(FakeHttpRequestMessage.withBody(payload), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("{\"error\":\"serialization_failed\"}");
    }
}
