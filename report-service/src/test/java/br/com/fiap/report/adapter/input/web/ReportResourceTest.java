package br.com.fiap.report.adapter.input.web;

import br.com.fiap.report.application.port.input.GenerateWeeklyReportUseCase;
import br.com.fiap.report.domain.WeeklyReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportResourceTest {

    private static final WeeklyReport REPORT = new WeeklyReport(
            OffsetDateTime.of(2026, 7, 21, 0, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2026, 7, 28, 0, 0, 0, 0, ZoneOffset.UTC),
            3, Map.of(), 1, List.of(), 3.0, 4.5);

    @Mock
    private GenerateWeeklyReportUseCase generateReport;

    @Test
    @DisplayName("POST /run triggers a full generation, including storage and e-mail")
    void runTriggersGeneration() {
        when(generateReport.generateLastWeek()).thenReturn(REPORT);

        assertThat(new ReportResource(generateReport).run()).isSameAs(REPORT);

        verify(generateReport).generateLastWeek();
        verify(generateReport, never()).previewLastWeek();
    }

    @Test
    @DisplayName("GET only previews, so it must not trigger a generation")
    void previewDoesNotGenerate() {
        when(generateReport.previewLastWeek()).thenReturn(REPORT);

        assertThat(new ReportResource(generateReport).preview()).isSameAs(REPORT);

        verify(generateReport).previewLastWeek();
        verify(generateReport, never()).generateLastWeek();
    }
}
