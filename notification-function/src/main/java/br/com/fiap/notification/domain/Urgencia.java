package br.com.fiap.notification.domain;

/** Severity of a critical event, from least to most urgent. */
public enum Urgencia {
    BAIXA,
    MEDIA,
    ALTA,
    CRITICA;

    /**
     * Parses the urgency accepted on the wire, case-insensitively.
     *
     * @throws InvalidNotificationException when the value is not one of the constants
     */
    public static Urgencia from(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidNotificationException("urgencia is required");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidNotificationException(
                    "urgencia must be one of BAIXA, MEDIA, ALTA, CRITICA (got '%s')".formatted(value));
        }
    }
}
