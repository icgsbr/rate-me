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
        sendCriticalAlert(notification, adminEmail);
    }

    @Override
    public void sendCriticalAlert(CriticalNotification notification, String recipient) {
        String subject = "[%s] Critical event on the rate-me platform".formatted(notification.urgencia());

        String body = """
                A critical event was reported and requires attention.

                Urgency:     %s
                Submitted:   %s
                Description: %s
                """.formatted(
                notification.urgencia(),
                TIMESTAMP.format(notification.dataEnvio()),
                notification.descricao());

        mailer.send(Mail.withText(recipient, subject, body));
        log.info("Critical alert e-mail sent to {} (urgencia={})", recipient, notification.urgencia());
    }
}
