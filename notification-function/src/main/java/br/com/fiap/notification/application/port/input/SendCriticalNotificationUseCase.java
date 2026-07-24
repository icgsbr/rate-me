package br.com.fiap.notification.application.port.input;

import br.com.fiap.notification.domain.CriticalNotification;

/** Use case for escalating a critical event to the administrators. */
public interface SendCriticalNotificationUseCase {

    void send(CriticalNotification notification);
}
