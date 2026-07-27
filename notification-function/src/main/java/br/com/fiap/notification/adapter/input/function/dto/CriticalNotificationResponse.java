package br.com.fiap.notification.adapter.input.function.dto;

import br.com.fiap.notification.domain.CriticalNotification;

import java.time.format.DateTimeFormatter;

/** What the caller gets back once the alert has been dispatched. */
public record CriticalNotificationResponse(
        String status,
        String urgencia,
        String dataEnvio
) {

    public static CriticalNotificationResponse sent(CriticalNotification notification) {
        return new CriticalNotificationResponse(
                "SENT",
                notification.urgencia().name(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(notification.dataEnvio()));
    }
}
