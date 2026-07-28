package br.com.fiap.notification.application.service;

import br.com.fiap.notification.application.port.input.ScanServiceErrorsUseCase;
import br.com.fiap.notification.application.port.output.AlertSenderPort;
import br.com.fiap.notification.application.port.output.LogQueryPort;
import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.ServiceErrorLog;
import br.com.fiap.notification.domain.Urgencia;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class ErrorLogScanService implements ScanServiceErrorsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogScanService.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final int MAX_DETAILED = 10;

    private final LogQueryPort logQuery;
    private final AlertSenderPort alertSender;
    private final String monitoredService;
    private final String recipient;

    public ErrorLogScanService(
            LogQueryPort logQuery,
            AlertSenderPort alertSender,
            @ConfigProperty(name = "app.log-scan.role-name") String monitoredService,
            @ConfigProperty(name = "app.notification.alert-admin-email") String recipient) {
        this.logQuery = logQuery;
        this.alertSender = alertSender;
        this.monitoredService = monitoredService;
        this.recipient = recipient;
    }

    @Override
    public int scan(OffsetDateTime from, OffsetDateTime to) {
        List<ServiceErrorLog> errors = logQuery.findErrorsBetween(from, to);
        if (errors.isEmpty()) {
            log.info("No {} errors between {} and {}", monitoredService, from, to);
            return 0;
        }

        log.warn("Found {} {} error(s) between {} and {} - alerting {}",
                errors.size(), monitoredService, from, to, recipient);

        alertSender.sendCriticalAlert(
                new CriticalNotification(describe(errors, from, to), Urgencia.ALTA, to),
                recipient);

        return errors.size();
    }

    private String describe(List<ServiceErrorLog> errors, OffsetDateTime from, OffsetDateTime to) {
        StringBuilder text = new StringBuilder()
                .append("%d ERROR-level record(s) found in the %s logs between %s and %s.%n%n"
                        .formatted(errors.size(), monitoredService, TIMESTAMP.format(from), TIMESTAMP.format(to)));

        errors.stream().limit(MAX_DETAILED).forEach(error -> text.append("- [%s] %s (%s, operation %s)%n".formatted(
                TIMESTAMP.format(error.timestamp()), error.detail(), error.kind(), error.operationId())));

        if (errors.size() > MAX_DETAILED) {
            text.append("%n... and %d more.%n".formatted(errors.size() - MAX_DETAILED));
        }

        String description = text.toString();
        return description.length() > CriticalNotification.MAX_DESCRICAO_LENGTH
                ? description.substring(0, CriticalNotification.MAX_DESCRICAO_LENGTH - 4) + "..."
                : description;
    }
}
