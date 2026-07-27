package br.com.fiap.notification.adapter.input.function.dto;

import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.InvalidNotificationException;
import br.com.fiap.notification.domain.Urgencia;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Alert payload accepted by the HTTP trigger. Field names follow the challenge wording
 * ({@code descricao}, {@code urgencia}, {@code dataEnvio}).
 *
 * <p>{@code dataEnvio} is optional and defaults to "now"; unknown properties are ignored so
 * that a richer caller (e.g. an Azure Monitor webhook wrapper) does not break the contract.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CriticalNotificationRequest(
        String descricao,
        String urgencia,
        String dataEnvio
) {

    public CriticalNotification toDomain() {
        return new CriticalNotification(descricao, Urgencia.from(urgencia), parseDataEnvio());
    }

    private OffsetDateTime parseDataEnvio() {
        if (dataEnvio == null || dataEnvio.isBlank()) {
            return null; // the domain fills in "now"
        }
        try {
            return OffsetDateTime.parse(dataEnvio);
        } catch (DateTimeParseException e) {
            throw new InvalidNotificationException(
                    "dataEnvio must be an ISO-8601 offset date-time (got '%s')".formatted(dataEnvio));
        }
    }
}
