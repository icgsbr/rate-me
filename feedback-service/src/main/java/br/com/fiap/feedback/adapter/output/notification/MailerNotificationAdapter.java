package br.com.fiap.feedback.adapter.output.notification;

import br.com.fiap.feedback.application.port.output.NotificationPort;
import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Student;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the low-score alert as an e-mail over SMTP (MailHog in development).
 *
 * <p>This is the only place that knows about e-mail. Swapping it for Azure Communication
 * Services later means replacing this adapter, leaving {@link NotificationPort} and the
 * application layer untouched.</p>
 */
@ApplicationScoped
public class MailerNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(MailerNotificationAdapter.class);

    private final Mailer mailer;
    private final String adminEmail;

    public MailerNotificationAdapter(
            Mailer mailer,
            @ConfigProperty(name = "app.notification.admin-email") String adminEmail) {
        this.mailer = mailer;
        this.adminEmail = adminEmail;
    }

    @Override
    public void notifyLowScore(Feedback feedback, Student student) {
        // The PDF requires the alert to carry: description, urgency and submission date.
        String subject = "[URGENT] Low score feedback received (score %d)".formatted(feedback.getScore());
        String body = """
                A critical feedback has been submitted and requires attention.

                Urgency:     CRITICAL (score %d, threshold %d)
                Student:     %s (%s)
                Description: %s
                Submitted:   %s
                """.formatted(
                feedback.getScore(),
                Feedback.LOW_SCORE_THRESHOLD,
                student.getName(),
                student.getId(),
                feedback.getReviewDescription(),
                feedback.getReviewDate());

        mailer.send(Mail.withText(adminEmail, subject, body));
        log.info("Low-score alert e-mail sent to {} for feedback {}", adminEmail, feedback.getId());
    }
}
