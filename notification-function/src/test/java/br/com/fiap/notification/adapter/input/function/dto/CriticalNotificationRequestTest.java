package br.com.fiap.notification.adapter.input.function.dto;

import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.InvalidNotificationException;
import br.com.fiap.notification.domain.Urgencia;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriticalNotificationRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("maps a fully populated request onto the domain record")
    void mapsFullRequest() {
        CriticalNotificationRequest request =
                new CriticalNotificationRequest("disk full", "alta", "2026-07-28T10:00:00Z");

        CriticalNotification notification = request.toDomain();

        assertThat(notification.descricao()).isEqualTo("disk full");
        assertThat(notification.urgencia()).isEqualTo(Urgencia.ALTA);
        assertThat(notification.dataEnvio())
                .isEqualTo(OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("leaves dataEnvio to the domain when it is absent or blank")
    void defersDataEnvioToTheDomain(String dataEnvio) {
        OffsetDateTime before = OffsetDateTime.now();

        CriticalNotification notification =
                new CriticalNotificationRequest("disk full", "ALTA", dataEnvio).toDomain();

        assertThat(notification.dataEnvio()).isBetween(before, OffsetDateTime.now());
    }

    @Test
    @DisplayName("rejects a dataEnvio that is not an ISO-8601 offset date-time")
    void rejectsMalformedDataEnvio() {
        CriticalNotificationRequest request =
                new CriticalNotificationRequest("disk full", "ALTA", "28/07/2026");

        assertThatThrownBy(request::toDomain)
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessage("dataEnvio must be an ISO-8601 offset date-time (got '28/07/2026')");
    }

    @Test
    @DisplayName("propagates the urgencia validation error from the domain")
    void rejectsUnknownUrgencia() {
        CriticalNotificationRequest request =
                new CriticalNotificationRequest("disk full", "URGENTISSIMO", null);

        assertThatThrownBy(request::toDomain)
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessageContaining("urgencia must be one of");
    }

    @Test
    @DisplayName("deserialises from JSON while ignoring unknown properties")
    void deserialisesIgnoringUnknownProperties() throws Exception {
        String json = """
                {"descricao":"disk full","urgencia":"ALTA","dataEnvio":"2026-07-28T10:00:00Z",
                 "origem":"feedback-service","severity":9}""";

        CriticalNotificationRequest request =
                objectMapper.readValue(json, CriticalNotificationRequest.class);

        assertThat(request.descricao()).isEqualTo("disk full");
        assertThat(request.urgencia()).isEqualTo("ALTA");
        assertThat(request.dataEnvio()).isEqualTo("2026-07-28T10:00:00Z");
    }
}
