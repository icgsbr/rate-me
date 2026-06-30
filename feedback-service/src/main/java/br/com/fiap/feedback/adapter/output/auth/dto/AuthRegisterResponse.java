package br.com.fiap.feedback.adapter.output.auth.dto;

/** Response body of {@code POST /auth/register}: the auth-service user id. */
public record AuthRegisterResponse(
        String id
) {}
