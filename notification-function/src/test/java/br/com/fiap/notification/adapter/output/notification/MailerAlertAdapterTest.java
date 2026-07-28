package br.com.fiap.notification.adapter.output.notification;

import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.Urgencia;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailerAlertAdapterTest {

    private static final String ADMIN_EMAIL = "admin@rate-me.test";

    private static final CriticalNotification NOTIFICATION = new CriticalNotification(
            "the feedback database is unreachable",
            Urgencia.CRITICA,
            OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC));

    @Mock
    private Mailer mailer;

    @Captor
    private ArgumentCaptor<Mail> mailCaptor;

    private MailerAlertAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MailerAlertAdapter(mailer, ADMIN_EMAIL);
    }

    @Test
    @DisplayName("sends to the configured admin address when no recipient is given")
    void sendsToTheConfiguredAdmin() {
        adapter.sendCriticalAlert(NOTIFICATION);

        verify(mailer).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getTo()).containsExactly(ADMIN_EMAIL);
    }

    @Test
    @DisplayName("sends to the explicit recipient when one is given")
    void sendsToTheExplicitRecipient() {
        adapter.sendCriticalAlert(NOTIFICATION, "oncall@rate-me.test");

        verify(mailer).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getTo()).containsExactly("oncall@rate-me.test");
    }

    @Test
    @DisplayName("puts the urgency in the subject and the full context in the body")
    void buildsAReadableAlert() {
        adapter.sendCriticalAlert(NOTIFICATION, "oncall@rate-me.test");

        verify(mailer).send(mailCaptor.capture());
        Mail mail = mailCaptor.getValue();

        assertThat(mail.getSubject()).isEqualTo("[CRITICA] Critical event on the rate-me platform");
        assertThat(mail.getText())
                .contains("Urgency:     CRITICA")
                .contains("Submitted:   2026-07-28T10:00:00Z")
                .contains("Description: the feedback database is unreachable");
    }
}
