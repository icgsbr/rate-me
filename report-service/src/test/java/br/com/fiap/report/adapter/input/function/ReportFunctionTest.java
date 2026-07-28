package br.com.fiap.report.adapter.input.function;

import br.com.fiap.report.application.port.input.GenerateWeeklyReportUseCase;
import br.com.fiap.report.domain.WeeklyReport;
import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportFunctionTest {

    private static final String TIMER_INFO = "{\"ScheduleStatus\":{\"Last\":\"2026-07-28T10:00:00Z\"}}";

    private static final WeeklyReport REPORT = new WeeklyReport(
            OffsetDateTime.of(2026, 7, 21, 0, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2026, 7, 28, 0, 0, 0, 0, ZoneOffset.UTC),
            0, Map.of(), 0, List.of(), 0.0, 0.0);

    @Mock
    private GenerateWeeklyReportUseCase generateReport;

    @Mock
    private ExecutionContext context;

    private ReportFunction function;

    @BeforeEach
    void setUp() {
        function = new ReportFunction();
        function.generateReport = generateReport;
        // The function logs unguarded through the context, so a Logger must be present.
        when(context.getLogger()).thenReturn(Logger.getLogger(ReportFunctionTest.class.getName()));
    }

    @Test
    @DisplayName("delegates the timer trigger to the weekly report use case")
    void runsTheWeeklyReport() {
        when(generateReport.generateLastWeek()).thenReturn(REPORT);

        function.execute(TIMER_INFO, context);

        verify(generateReport).generateLastWeek();
    }

    @Test
    @DisplayName("wraps a generation failure so the Functions host marks the run as failed")
    void wrapsGenerationFailure() {
        when(generateReport.generateLastWeek()).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> function.execute(TIMER_INFO, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Falha na execução do relatório: database down")
                .cause().isInstanceOf(IllegalStateException.class);
    }
}
