package br.com.fiap.notification.application.service;

import br.com.fiap.notification.application.port.input.SendCriticalNotificationUseCase;
import br.com.fiap.notification.application.port.output.AlertSenderPort;
import br.com.fiap.notification.domain.CriticalNotification;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CriticalNotificationService implements SendCriticalNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CriticalNotificationService.class);

    private final AlertSenderPort alertSender;

    public CriticalNotificationService(AlertSenderPort alertSender) {
        this.alertSender = alertSender;
    }

    @Override
    public void send(CriticalNotification notification) {
        log.info("Dispatching critical alert (urgencia={}, dataEnvio={})",
                notification.urgencia(), notification.dataEnvio());
        alertSender.sendCriticalAlert(notification);
    }
}
