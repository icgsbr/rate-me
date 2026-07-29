package br.com.fiap.report.application.port.input;

import br.com.fiap.report.domain.WeeklyReport;

import java.time.OffsetDateTime;

public interface GenerateWeeklyReportUseCase {

    WeeklyReport generate(OffsetDateTime from, OffsetDateTime to);

    WeeklyReport generateLastWeek();

    WeeklyReport previewLastWeek();
}
