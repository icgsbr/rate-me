package br.com.fiap.notification.adapter.input.function.dto;

import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.Urgencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalNotificationResponseTest {

    @Test
    @DisplayName("reports SENT with the urgency name and an ISO-8601 timestamp")
    void buildsSentResponse() {
        CriticalNotification notification = new CriticalNotification(
                "disk full",
                Urgencia.MEDIA,
                OffsetDateTime.of(2026, 7, 28, 10, 30, 0, 0, ZoneOffset.UTC));

        CriticalNotificationResponse response = CriticalNotificationResponse.sent(notification);

        assertThat(response.status()).isEqualTo("SENT");
        assertThat(response.urgencia()).isEqualTo("MEDIA");
        assertThat(response.dataEnvio()).isEqualTo("2026-07-28T10:30:00Z");
    }
}
