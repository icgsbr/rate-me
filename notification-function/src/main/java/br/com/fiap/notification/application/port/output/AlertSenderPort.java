package br.com.fiap.notification.application.port.output;

import br.com.fiap.notification.domain.CriticalNotification;

/**
 * Output port for delivering the alert. E-mail over SMTP today; swapping it for Azure
 * Communication Services later means replacing the adapter, not the application layer.
 */
public interface AlertSenderPort {

    /** Sends to the default administrator address. */
    void sendCriticalAlert(CriticalNotification notification);

    /**
     * Sends to an explicit address. The log scan uses this so its alerts can be routed to a
     * different mailbox than the ones raised through {@code POST /api/notifications/critical}.
     */
    void sendCriticalAlert(CriticalNotification notification, String recipient);
}
