package br.com.fiap.feedback.adapter.input.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank String login,
        @NotBlank String password,
        String registrationNumber,
        String role
) {}
