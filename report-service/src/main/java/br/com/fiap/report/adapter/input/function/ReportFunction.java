package br.com.fiap.report.adapter.input.function;

import br.com.fiap.report.application.port.input.GenerateWeeklyReportUseCase;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.logging.Level;

/**
 * Ponto de entrada da Azure Function para acionar o relatório semanal.
 */
@Named("report")
public class ReportFunction {

    @Inject
    GenerateWeeklyReportUseCase generateReport;

    @FunctionName("generateWeeklyReport")
    public void execute(
            @TimerTrigger(name = "timerInfo", schedule = "%CRON_SCHEDULE%") String timerInfo,
            final ExecutionContext context) {

        context.getLogger().info("=== INÍCIO: Gatilho de tempo acionado! TimerInfo: " + timerInfo + " ===");

        try {

            context.getLogger().info("Chamando o UseCase generateLastWeek()...");

            generateReport.generateLastWeek();

            context.getLogger().info("=== SUCESSO: Geração de relatório semanal finalizada sem erros! ===");

        } catch (Exception e) {

            context.getLogger().log(Level.SEVERE, "=== ERRO FATAL: Falha ao gerar o relatório semanal ===", e);

            throw new RuntimeException("Falha na execução do relatório: " + e.getMessage(), e);
        }
    }
}