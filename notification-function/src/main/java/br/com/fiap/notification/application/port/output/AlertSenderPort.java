package br.com.fiap.notification.application.port.output;

import br.com.fiap.notification.domain.CriticalNotification;

public interface AlertSenderPort {

    void sendCriticalAlert(CriticalNotification notification);

    void sendCriticalAlert(CriticalNotification notification, String recipient);
}
