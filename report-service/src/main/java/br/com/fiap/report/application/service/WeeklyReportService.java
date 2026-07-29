package br.com.fiap.report.application.service;

import br.com.fiap.report.application.port.input.GenerateWeeklyReportUseCase;
import br.com.fiap.report.application.port.output.FeedbackQueryPort;
import br.com.fiap.report.application.port.output.FeedbackQueryPort.FeedbackRow;
import br.com.fiap.report.application.port.output.ReportNotificationPort;
import br.com.fiap.report.application.port.output.ReportStoragePort;
import br.com.fiap.report.domain.WeeklyReport;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class WeeklyReportService implements GenerateWeeklyReportUseCase {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportService.class);

    private static final int LOW_SCORE_THRESHOLD = 1;
    private static final int DEFAULT_PERIOD_DAYS = 7;

    private final FeedbackQueryPort feedbackQuery;
    private final ReportNotificationPort notification;
    private final ReportStoragePort storage;

    public WeeklyReportService(FeedbackQueryPort feedbackQuery,
                               ReportNotificationPort notification,
                               ReportStoragePort storage) {
        this.feedbackQuery = feedbackQuery;
        this.notification = notification;
        this.storage = storage;
    }

    @Override
    public WeeklyReport generateLastWeek() {
        OffsetDateTime to = OffsetDateTime.now();
        OffsetDateTime from = to.minusDays(DEFAULT_PERIOD_DAYS);
        return generate(from, to);
    }

    @Override
    public WeeklyReport previewLastWeek() {
        OffsetDateTime to = OffsetDateTime.now();
        OffsetDateTime from = to.minusDays(DEFAULT_PERIOD_DAYS);
        log.info("Previewing weekly report for period {} .. {}", from, to);
        return buildReport(from, to, feedbackQuery.findBetween(from, to));
    }

    @Override
    public WeeklyReport generate(OffsetDateTime from, OffsetDateTime to) {
        log.info("Generating weekly report for period {} .. {}", from, to);

        List<FeedbackRow> rows = feedbackQuery.findBetween(from, to);
        WeeklyReport report = buildReport(from, to, rows);

        storage.store(report);
        notification.sendWeeklyReport(report);

        log.info("Weekly report generated: {} evaluations, {} low-score",
                report.totalEvaluations(), report.lowScoreCount());
        return report;
    }

    private WeeklyReport buildReport(OffsetDateTime from, OffsetDateTime to, List<FeedbackRow> rows) {
        long total = rows.size();

        Map<LocalDate, Long> perDay = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.reviewDate().atZoneSameInstant(ZoneOffset.UTC).toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()));

        List<FeedbackRow> lowScores = rows.stream()
                .filter(r -> r.score() <= LOW_SCORE_THRESHOLD)
                .toList();

        List<UUID> lowScoreStudents = lowScores.stream()
                .map(FeedbackRow::studentId)
                .distinct()
                .toList();

        double averageScore = rows.stream()
                .mapToInt(FeedbackRow::score)
                .average()
                .orElse(0.0);

        long days = Math.max(1, Duration.between(from, to).toDays());
        double weeks = Math.max(1.0, days / 7.0);
        double avgPerWeek = total / weeks;

        return new WeeklyReport(
                from, to, total, perDay,
                lowScores.size(), lowScoreStudents,
                avgPerWeek, averageScore);
    }
}
