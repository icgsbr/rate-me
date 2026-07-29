package br.com.fiap.notification.domain;

public enum Urgencia {
    BAIXA,
    MEDIA,
    ALTA,
    CRITICA;

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
