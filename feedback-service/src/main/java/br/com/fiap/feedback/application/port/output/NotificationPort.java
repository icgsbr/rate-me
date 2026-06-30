package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Student;

/**
 * Output port for outbound notifications.
 *
 * <p>The local implementation sends an e-mail via SMTP (MailHog in development). In the
 * cloud this adapter is swapped for Azure Communication Services without touching the
 * application layer.</p>
 */
public interface NotificationPort {

    /** Alerts the administrator about a critical (low) score. */
    void notifyLowScore(Feedback feedback, Student student);
}
