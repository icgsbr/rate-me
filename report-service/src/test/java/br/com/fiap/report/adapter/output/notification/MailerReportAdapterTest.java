package br.com.fiap.report.adapter.output.notification;

import br.com.fiap.report.domain.WeeklyReport;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailerReportAdapterTest {

    private static final String RECIPIENT = "reports@rate-me.test";

    private static final OffsetDateTime START = OffsetDateTime.of(2026, 7, 21, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END = OffsetDateTime.of(2026, 7, 28, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private Mailer mailer;

    @Captor
    private ArgumentCaptor<Mail> mailCaptor;

    private MailerReportAdapter adapter;
    private Locale originalLocale;

    @BeforeEach
    void setUp() {
        // render() formats decimals with %.2f, which follows the default locale.
        // Pin it so the assertions do not depend on the machine running the build.
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        adapter = new MailerReportAdapter(mailer, RECIPIENT);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    @DisplayName("renders the figures and the per-day breakdown into the e-mail body")
    void rendersAPopulatedReport() {
        Map<LocalDate, Long> perDay = new TreeMap<>(Map.of(
                LocalDate.of(2026, 7, 22), 2L,
                LocalDate.of(2026, 7, 24), 1L));

        adapter.sendWeeklyReport(new WeeklyReport(
                START, END, 3, perDay, 1, List.of(ALICE), 3.0, 4.25));

        verify(mailer).send(mailCaptor.capture());
        Mail mail = mailCaptor.getValue();

        assertThat(mail.getTo()).containsExactly(RECIPIENT);
        assertThat(mail.getSubject()).isEqualTo("Weekly feedback report");
        assertThat(mail.getText())
                .contains("Total evaluations:          3")
                .contains("Average evaluations/week:   3.00")
                .contains("Average score (period):     4.25")
                .contains("Low-score evaluations:      1")
                .contains(ALICE.toString())
                .contains("  2026-07-22: 2")
                .contains("  2026-07-24: 1")
                .doesNotContain("(no evaluations)")
                .doesNotContain("(none)");
    }

    @Test
    @DisplayName("states explicitly that there were no evaluations and no low scores")
    void rendersAnEmptyReport() {
        adapter.sendWeeklyReport(new WeeklyReport(
                START, END, 0, Map.of(), 0, List.of(), 0.0, 0.0));

        verify(mailer).send(mailCaptor.capture());

        assertThat(mailCaptor.getValue().getText())
                .contains("  (no evaluations)")
                .contains("Low-score student ids:      (none)")
                .contains("Average score (period):     0.00");
    }
}
