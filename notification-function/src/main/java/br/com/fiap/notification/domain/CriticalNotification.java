package br.com.fiap.notification.domain;

import java.time.OffsetDateTime;

/**
 * A critical event that must be escalated to the administrators.
 *
 * <p>Carries exactly the three fields the challenge requires in the urgency e-mail:
 * description, urgency and submission date.</p>
 *
 * @param descricao  what happened (free text)
 * @param urgencia   how severe it is
 * @param dataEnvio  when the event was raised
 */
public record CriticalNotification(
        String descricao,
        Urgencia urgencia,
        OffsetDateTime dataEnvio
) {

    /** Longest description accepted, so an oversized payload cannot bloat the e-mail. */
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
        // The caller may omit the date; the moment we received the event is a fair default.
        if (dataEnvio == null) {
            dataEnvio = OffsetDateTime.now();
        }
    }
}
