package br.com.fiap.notification.domain;

import java.time.OffsetDateTime;

public record CriticalNotification(
        String descricao,
        Urgencia urgencia,
        OffsetDateTime dataEnvio
) {

    public static final int MAX_DESCRICAO_LENGTH = 2000;

    public CriticalNotification {
        if (descricao == null || descricao.isBlank()) {
            throw new InvalidNotificationException("descricao is required");
        }
        if (descricao.length() > MAX_DESCRICAO_LENGTH) {
            throw new InvalidNotificationException(
                    "descricao must be at most %d characters".formatted(MAX_DESCRICAO_LENGTH));
        }
        if (urgencia == null) {
            throw new InvalidNotificationException("urgencia is required");
        }
        if (dataEnvio == null) {
            dataEnvio = OffsetDateTime.now();
        }
    }
}
