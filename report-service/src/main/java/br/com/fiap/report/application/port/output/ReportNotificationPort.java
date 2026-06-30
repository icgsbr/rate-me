package br.com.fiap.report.application.port.output;

import br.com.fiap.report.domain.WeeklyReport;

/** Output port for delivering the weekly report (e-mail locally). */
public interface ReportNotificationPort {

    void sendWeeklyReport(WeeklyReport report);
}
