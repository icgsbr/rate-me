package br.com.fiap.notification.application.service;

import br.com.fiap.notification.application.port.output.AlertSenderPort;
import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.Urgencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CriticalNotificationServiceTest {

    private static final CriticalNotification NOTIFICATION = new CriticalNotification(
            "database unreachable",
            Urgencia.CRITICA,
            OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC));

    @Mock
    private AlertSenderPort alertSender;

    @Test
    @DisplayName("delegates to the default-recipient overload of the alert sender")
    void delegatesToDefaultRecipientOverload() {
        new CriticalNotificationService(alertSender).send(NOTIFICATION);

        verify(alertSender).sendCriticalAlert(NOTIFICATION);
        verify(alertSender, never()).sendCriticalAlert(any(CriticalNotification.class), anyString());
    }

    @Test
    @DisplayName("lets a delivery failure bubble up to the caller")
    void propagatesDeliveryFailure() {
        doThrow(new IllegalStateException("smtp down")).when(alertSender).sendCriticalAlert(NOTIFICATION);

        assertThatThrownBy(() -> new CriticalNotificationService(alertSender).send(NOTIFICATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("smtp down");
    }
}
