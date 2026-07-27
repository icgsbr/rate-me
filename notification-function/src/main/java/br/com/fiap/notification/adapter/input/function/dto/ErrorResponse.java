package br.com.fiap.notification.adapter.input.function.dto;

/** Error body returned when an alert cannot be accepted or delivered. */
public record ErrorResponse(String error, String message) {
}
