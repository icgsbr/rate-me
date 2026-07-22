package br.com.fiap.report.adapter.input.function;

import br.com.fiap.report.application.port.input.GenerateWeeklyReportUseCase;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada da Azure Function para acionar o relatório semanal.
 * Esta classe substitui o {@code WeeklyReportScheduler} local.
 */
@Named("report")
public class ReportFunction {

    private static final Logger log = LoggerFactory.getLogger(ReportFunction.class);

    @Inject
    GenerateWeeklyReportUseCase generateReport;

    /**
     * Esta função é acionada por um Timer do Azure. A expressão cron na
     * anotação {@code @TimerTrigger} define o agendamento (toda segunda-feira às 8:00 AM UTC).
     */
    @FunctionName("generateWeeklyReport")
    public void execute(
            @TimerTrigger(name = "timerInfo", schedule = "%app.report.cloud.cron%") String timerInfo,
            final ExecutionContext context) {
        log.info("Gatilho de tempo da Azure Function recebido: {}", timerInfo);
        generateReport.generateLastWeek();
        log.info("Geração de relatório semanal iniciada pela Azure Function.");
    }
}