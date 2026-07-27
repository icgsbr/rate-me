package br.com.fiap.notification.domain;

import java.time.OffsetDateTime;

/**
 * One error-level record observed in another service's logs.
 *
 * <p>Deliberately generic: the scan only needs when it happened, whether it came from a
 * log statement or from an unhandled exception, and enough text to put in the e-mail.</p>
 *
 * @param timestamp   when the service produced the record
 * @param kind        {@code trace} for a logger at ERROR, {@code exception} for a stack trace
 * @param detail      the message (or {@code type :: message} for an exception)
 * @param operationId correlation id, so the reader can find the request in Application Insights
 */
public record ServiceErrorLog(
        OffsetDateTime timestamp,
        String kind,
        String detail,
        String operationId
) {

    /** Longest detail kept per line, so one huge stack trace cannot eat the whole e-mail. */
    public static final int MAX_DETAIL_LENGTH = 300;

    public ServiceErrorLog {
        if (detail != null && detail.length() > MAX_DETAIL_LENGTH) {
            detail = detail.substring(0, MAX_DETAIL_LENGTH) + "...";
        }
    }
}
