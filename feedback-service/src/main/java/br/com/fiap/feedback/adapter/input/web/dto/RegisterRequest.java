package br.com.fiap.feedback.adapter.input.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Registration payload for {@code POST /cadastro}.
 *
 * <p>{@code role} is optional and defaults to {@code STUDENT}; {@code registrationNumber}
 * is only meaningful for students.</p>
 */
public record RegisterRequest(
        @NotBlank String name,
        @NotBlank String login,
        @NotBlank String password,
        String registrationNumber,
        String role
) {}
