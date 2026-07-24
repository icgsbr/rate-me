package br.com.fiap.notification.adapter.output.notification;

import br.com.fiap.notification.application.port.output.AlertSenderPort;
import br.com.fiap.notification.domain.CriticalNotification;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

/**
 * Sends the critical alert as an e-mail over SMTP.
 *
 * <p>This is the only place that knows about e-mail; it mirrors the body format used by
 * {@code feedback-service}'s low-score alert so both alerts read the same.</p>
 */
@ApplicationScoped
public class MailerAlertAdapter implements AlertSenderPort {

    private static final Logger log = LoggerFactory.getLogger(MailerAlertAdapter.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Mailer mailer;
    private final String adminEmail;

    public MailerAlertAdapter(
            Mailer mailer,
            @ConfigProperty(name = "app.notification.admin-email") String adminEmail) {
        this.mailer = mailer;
        this.adminEmail = adminEmail;
    }

    @Override
    public void sendCriticalAlert(CriticalNotification notification) {
        String subject = "[%s] Critical event on the rate-me platform".formatted(notification.urgencia());

        // The challenge requires the alert to carry: description, urgency and submission date.
        String body = """
                A critical event was reported and requires attention.

                Urgency:     %s
                Submitted:   %s
                Description: %s
                """.formatted(
                notification.urgencia(),
                TIMESTAMP.format(notification.dataEnvio()),
                notification.descricao());

        mailer.send(Mail.withText(adminEmail, subject, body));
        log.info("Critical alert e-mail sent to {} (urgencia={})", adminEmail, notification.urgencia());
    }
}
