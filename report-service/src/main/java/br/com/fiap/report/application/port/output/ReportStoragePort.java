package br.com.fiap.report.application.port.output;

import br.com.fiap.report.domain.WeeklyReport;

public interface ReportStoragePort {

    void store(WeeklyReport report);
}
