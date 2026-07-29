package br.com.fiap.report.adapter.output.storage;

import br.com.fiap.report.domain.WeeklyReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingReportStorageAdapterTest {

    @Test
    @DisplayName("accepts a report without failing (placeholder until a real store exists)")
    void storesWithoutFailing() {
        WeeklyReport report = new WeeklyReport(
                OffsetDateTime.of(2026, 7, 21, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 7, 28, 0, 0, 0, 0, ZoneOffset.UTC),
                2, Map.of(), 0, List.of(), 2.0, 7.5);

        assertThatCode(() -> new LoggingReportStorageAdapter().store(report)).doesNotThrowAnyException();
    }
}
