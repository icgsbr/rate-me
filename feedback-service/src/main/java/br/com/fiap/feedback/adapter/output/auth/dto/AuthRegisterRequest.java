package br.com.fiap.feedback.adapter.output.auth.dto;

/** Request body for {@code POST /auth/register} on the auth-service. */
public record AuthRegisterRequest(
        String login,
        String password,
        String externalId
) {}
