package br.com.fiap.report.application.port.input;

import br.com.fiap.report.domain.WeeklyReport;

import java.time.OffsetDateTime;

/** Use case for building (and dispatching) the weekly feedback report. */
public interface GenerateWeeklyReportUseCase {

    /** Builds the report for an explicit period, then stores and e-mails it. */
    WeeklyReport generate(OffsetDateTime from, OffsetDateTime to);

    /** Convenience: builds the report for the last 7 days up to now. */
    WeeklyReport generateLastWeek();
}
