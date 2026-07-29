package br.com.fiap.report.application.port.output;

import br.com.fiap.report.domain.WeeklyReport;

public interface ReportNotificationPort {

    void sendWeeklyReport(WeeklyReport report);
}
