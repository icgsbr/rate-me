package br.com.fiap.feedback.adapter.output.auth.dto;

public record AuthRegisterRequest(
        String login,
        String password,
        String externalId
) {}
