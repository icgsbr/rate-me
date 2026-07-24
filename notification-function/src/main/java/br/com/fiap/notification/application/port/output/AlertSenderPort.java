package br.com.fiap.notification.application.port.output;

import br.com.fiap.notification.domain.CriticalNotification;

/**
 * Output port for delivering the alert. E-mail over SMTP today; swapping it for Azure
 * Communication Services later means replacing the adapter, not the application layer.
 */
public interface AlertSenderPort {

    void sendCriticalAlert(CriticalNotification notification);
}
