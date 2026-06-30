package br.com.fiap.report.adapter.input.web;

import br.com.fiap.report.application.port.input.GenerateWeeklyReportUseCase;
import br.com.fiap.report.domain.WeeklyReport;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * On-demand report endpoint, used to verify the flow locally (the production trigger is
 * the scheduled job / Azure Timer Trigger). Both routes build the last-7-days report,
 * e-mail it and return the JSON.
 */
@Path("/reports/weekly")
@Produces(MediaType.APPLICATION_JSON)
public class ReportResource {

    private final GenerateWeeklyReportUseCase generateReport;

    public ReportResource(GenerateWeeklyReportUseCase generateReport) {
        this.generateReport = generateReport;
    }

    @POST
    @Path("/run")
    public WeeklyReport run() {
        return generateReport.generateLastWeek();
    }

    @GET
    public WeeklyReport preview() {
        return generateReport.generateLastWeek();
    }
}
