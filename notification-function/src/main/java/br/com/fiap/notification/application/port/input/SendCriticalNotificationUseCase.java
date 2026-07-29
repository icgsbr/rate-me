package br.com.fiap.notification.application.port.input;

import br.com.fiap.notification.domain.CriticalNotification;

public interface SendCriticalNotificationUseCase {

    void send(CriticalNotification notification);
}
