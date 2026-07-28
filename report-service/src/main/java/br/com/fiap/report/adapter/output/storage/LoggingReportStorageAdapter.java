package br.com.fiap.report.adapter.output.storage;

import br.com.fiap.report.application.port.output.ReportStoragePort;
import br.com.fiap.report.domain.WeeklyReport;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LoggingReportStorageAdapter implements ReportStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LoggingReportStorageAdapter.class);

    @Override
    public void store(WeeklyReport report) {

        log.info("[NoSQL placeholder] storing weekly report: {}", report);
    }
}
