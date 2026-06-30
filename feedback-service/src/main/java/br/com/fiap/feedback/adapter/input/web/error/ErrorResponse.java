package br.com.fiap.feedback.adapter.input.web.error;

/** Uniform error body returned by the exception mappers. */
public record ErrorResponse(
        String error,
        String message
) {}
