package br.com.fiap.report.application.port.output;

import br.com.fiap.report.domain.WeeklyReport;

/**
 * Output port for persisting generated reports.
 *
 * <p>Locally backed by a logging placeholder; in the cloud this is swapped for a NoSQL
 * store (Azure Cosmos DB) without touching the application layer.</p>
 */
public interface ReportStoragePort {

    void store(WeeklyReport report);
}
