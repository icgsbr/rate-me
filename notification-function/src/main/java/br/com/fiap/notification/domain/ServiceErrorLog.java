package br.com.fiap.notification.domain;

import java.time.OffsetDateTime;

public record ServiceErrorLog(
        OffsetDateTime timestamp,
        String kind,
        String detail,
        String operationId
) {

    public static final int MAX_DETAIL_LENGTH = 300;

    public ServiceErrorLog {
        if (detail != null && detail.length() > MAX_DETAIL_LENGTH) {
            detail = detail.substring(0, MAX_DETAIL_LENGTH) + "...";
        }
    }
}
