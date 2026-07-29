package br.com.fiap.feedback.adapter.output.notification;

import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Student;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailerNotificationAdapterTest {

    private static final String ADMIN_EMAIL = "admin@rate-me.test";
    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime REVIEWED_AT =
            OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);

    private static final Student STUDENT = Student.builder()
            .id(STUDENT_ID).name("Alice").registrationNumber("RM12345").build();

    @Mock
    private Mailer mailer;

    @Captor
    private ArgumentCaptor<Mail> mailCaptor;

    private MailerNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MailerNotificationAdapter(mailer, ADMIN_EMAIL);
    }

    @Test
    @DisplayName("puts the score in the subject and the full context in the body")
    void buildsTheLowScoreAlert() {
        adapter.notifyLowScore(feedback(0), STUDENT);

        verify(mailer).send(mailCaptor.capture());
        Mail mail = mailCaptor.getValue();

        assertThat(mail.getTo()).containsExactly(ADMIN_EMAIL);
        assertThat(mail.getSubject()).isEqualTo("[URGENT] Low score feedback received (score 0)");
        assertThat(mail.getText())
                .contains("Urgency:     CRITICAL (score 0, threshold 1)")
                .contains("Student:     Alice (" + STUDENT_ID + ")")
                .contains("Description: the pace was too fast")
                .contains("Submitted:   " + REVIEWED_AT);
    }

    @Test
    @DisplayName("reflects the actual score in the subject line")
    void reportsTheThresholdScore() {
        adapter.notifyLowScore(feedback(1), STUDENT);

        verify(mailer).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getSubject())
                .isEqualTo("[URGENT] Low score feedback received (score 1)");
    }

    private static Feedback feedback(int score) {
        return Feedback.builder()
                .id(7L)
                .studentId(STUDENT_ID)
                .courseId(UUID.randomUUID())
                .score(score)
                .reviewDescription("the pace was too fast")
                .reviewDate(REVIEWED_AT)
                .notified(false)
                .build();
    }
}
