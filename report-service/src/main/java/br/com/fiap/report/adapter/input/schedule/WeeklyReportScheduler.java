package br.com.fiap.report.adapter.input.schedule;

import br.com.fiap.report.application.port.input.GenerateWeeklyReportUseCase;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Triggers the weekly report on a schedule.
 *
 * <p>Locally this is driven by Quarkus' scheduler with a configurable cron
 * ({@code app.report.cron}, disabled by default). In the cloud the same use case is
 * invoked by an <strong>Azure Timer Trigger</strong> (serverless function with a single
 * responsibility), which replaces this in-process scheduler.</p>
 */
@ApplicationScoped
public class WeeklyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportScheduler.class);

    private final GenerateWeeklyReportUseCase generateReport;

    public WeeklyReportScheduler(GenerateWeeklyReportUseCase generateReport) {
        this.generateReport = generateReport;
    }

    // Disabled by default ("off"); set app.report.cron to e.g. "0 0 8 ? * MON" to enable.
    @Scheduled(cron = "{app.report.cron}")
    void runWeekly() {
        log.info("Scheduled weekly report triggered");
        generateReport.generateLastWeek();
    }
}
