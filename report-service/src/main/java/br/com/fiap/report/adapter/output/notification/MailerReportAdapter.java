package br.com.fiap.report.adapter.output.notification;

import br.com.fiap.report.application.port.output.ReportNotificationPort;
import br.com.fiap.report.domain.WeeklyReport;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

@ApplicationScoped
public class MailerReportAdapter implements ReportNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(MailerReportAdapter.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Mailer mailer;
    private final String recipient;

    public MailerReportAdapter(
            Mailer mailer,
            @ConfigProperty(name = "app.report.recipient-email") String recipient) {
        this.mailer = mailer;
        this.recipient = recipient;
    }

    @Override
    public void sendWeeklyReport(WeeklyReport report) {
        mailer.send(Mail.withText(recipient, "Weekly feedback report", render(report)));
        log.info("Weekly report e-mail sent to {}", recipient);
    }

    private String render(WeeklyReport report) {
        StringJoiner perDay = new StringJoiner("\n");
        report.evaluationsPerDay().forEach((day, count) ->
                perDay.add("  %s: %d".formatted(DATE.format(day), count)));
        if (report.evaluationsPerDay().isEmpty()) {
            perDay.add("  (no evaluations)");
        }

        return """
                Weekly feedback report
                Period: %s .. %s

                Total evaluations:          %d
                Average evaluations/week:   %.2f
                Average score (period):     %.2f
                Low-score evaluations:      %d
                Low-score student ids:      %s

                Evaluations per day:
                %s
                """.formatted(
                report.periodStart(),
                report.periodEnd(),
                report.totalEvaluations(),
                report.averageEvaluationsPerWeek(),
                report.averageScore(),
                report.lowScoreCount(),
                report.lowScoreStudentIds().isEmpty() ? "(none)" : report.lowScoreStudentIds(),
                perDay);
    }
}
