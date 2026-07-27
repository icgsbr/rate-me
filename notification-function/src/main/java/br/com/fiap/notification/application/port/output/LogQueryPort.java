package br.com.fiap.notification.application.port.output;

import br.com.fiap.notification.domain.ServiceErrorLog;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Output port for reading another service's error logs.
 *
 * <p>Application Insights today; pointing it at a Log Analytics workspace or a different
 * backend later means replacing the adapter, not the application layer.</p>
 */
public interface LogQueryPort {

    /**
     * Errors produced in the half-open window {@code (from, to]}.
     *
     * @return the records ordered oldest first, empty when the window was quiet
     */
    List<ServiceErrorLog> findErrorsBetween(OffsetDateTime from, OffsetDateTime to);
}
