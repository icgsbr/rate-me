package br.com.fiap.notification.application.port.input;

import java.time.OffsetDateTime;

/**
 * Sweeps the monitored service's logs for error-level records and escalates whatever it
 * finds to the administrators.
 */
public interface ScanServiceErrorsUseCase {

    /**
     * Scans the half-open window {@code (from, to]} and alerts when it is not empty.
     *
     * @return how many error records were found
     */
    int scan(OffsetDateTime from, OffsetDateTime to);
}
