package br.com.fiap.notification.application.service;

import br.com.fiap.notification.application.port.output.AlertSenderPort;
import br.com.fiap.notification.application.port.output.LogQueryPort;
import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.ServiceErrorLog;
import br.com.fiap.notification.domain.Urgencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorLogScanServiceTest {

    private static final String MONITORED_SERVICE = "feedback-service";
    private static final String RECIPIENT = "ops@rate-me.test";

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 7, 28, 9, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private LogQueryPort logQuery;

    @Mock
    private AlertSenderPort alertSender;

    @Captor
    private ArgumentCaptor<CriticalNotification> notificationCaptor;

    private ErrorLogScanService service;

    @BeforeEach
    void setUp() {
        service = new ErrorLogScanService(logQuery, alertSender, MONITORED_SERVICE, RECIPIENT);
    }

    @Test
    @DisplayName("returns zero and sends nothing when the window has no errors")
    void doesNotAlertWhenThereAreNoErrors() {
        when(logQuery.findErrorsBetween(FROM, TO)).thenReturn(List.of());

        int found = service.scan(FROM, TO);

        assertThat(found).isZero();
        verifyNoInteractions(alertSender);
    }

    @Test
    @DisplayName("sends an ALTA alert to the configured recipient and returns the error count")
    void alertsWhenErrorsAreFound() {
        when(logQuery.findErrorsBetween(FROM, TO)).thenReturn(List.of(
                error(1, "NullPointerException :: boom"),
                error(2, "timeout talking to auth-service")));

        int found = service.scan(FROM, TO);

        assertThat(found).isEqualTo(2);
        verify(alertSender).sendCriticalAlert(notificationCaptor.capture(), eq(RECIPIENT));

        CriticalNotification sent = notificationCaptor.getValue();
        assertThat(sent.urgencia()).isEqualTo(Urgencia.ALTA);
        assertThat(sent.dataEnvio()).isEqualTo(TO);
        assertThat(sent.descricao())
                .contains("2 ERROR-level record(s) found in the feedback-service logs")
                .contains("NullPointerException :: boom")
                .contains("timeout talking to auth-service")
                .contains("operation op-1")
                .doesNotContain("... and");
    }

    @Test
    @DisplayName("itemises at most ten errors and summarises the remainder")
    void summarisesBeyondTheTenthError() {
        List<ServiceErrorLog> errors = IntStream.rangeClosed(1, 13)
                .mapToObj(i -> error(i, "failure number " + i))
                .toList();
        when(logQuery.findErrorsBetween(FROM, TO)).thenReturn(errors);

        int found = service.scan(FROM, TO);

        assertThat(found).isEqualTo(13);
        verify(alertSender).sendCriticalAlert(notificationCaptor.capture(), eq(RECIPIENT));

        String description = notificationCaptor.getValue().descricao();
        assertThat(description)
                .contains("failure number 10")
                .doesNotContain("failure number 11")
                .contains("... and 3 more.");
    }

    @Test
    @DisplayName("truncates the description so it never breaches the domain limit")
    void truncatesAnOversizedDescription() {
        // Ten near-maximum details easily exceed the 2000-character cap, which would make
        // the CriticalNotification constructor reject the alert if it were not truncated.
        List<ServiceErrorLog> errors = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> error(i, "x".repeat(ServiceErrorLog.MAX_DETAIL_LENGTH)))
                .toList();
        when(logQuery.findErrorsBetween(FROM, TO)).thenReturn(errors);

        service.scan(FROM, TO);

        verify(alertSender).sendCriticalAlert(notificationCaptor.capture(), eq(RECIPIENT));

        String description = notificationCaptor.getValue().descricao();
        assertThat(description)
                .hasSize(CriticalNotification.MAX_DESCRICAO_LENGTH - 1)
                .endsWith("...");
    }

    @Test
    @DisplayName("propagates a log query failure instead of swallowing it")
    void propagatesLogQueryFailure() {
        when(logQuery.findErrorsBetween(FROM, TO)).thenThrow(new IllegalStateException("app id missing"));

        assertThatThrownBy(() -> service.scan(FROM, TO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app id missing");

        verifyNoInteractions(alertSender);
    }

    @Test
    @DisplayName("never uses the single-argument overload, so the alert e-mail is explicit")
    void alwaysTargetsAnExplicitRecipient() {
        when(logQuery.findErrorsBetween(FROM, TO)).thenReturn(List.of(error(1, "boom")));

        service.scan(FROM, TO);

        verify(alertSender).sendCriticalAlert(any(CriticalNotification.class), eq(RECIPIENT));
        verify(alertSender, org.mockito.Mockito.never()).sendCriticalAlert(any(CriticalNotification.class));
    }

    private static ServiceErrorLog error(int index, String detail) {
        return new ServiceErrorLog(FROM.plusMinutes(index), "trace", detail, "op-" + index);
    }
}
