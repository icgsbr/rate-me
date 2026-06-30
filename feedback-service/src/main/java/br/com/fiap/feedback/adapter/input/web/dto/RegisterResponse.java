package br.com.fiap.feedback.adapter.input.web.dto;

import br.com.fiap.feedback.application.port.input.RegisterUserUseCase.RegisterUserResult;

import java.util.UUID;

/** Result of a successful registration. */
public record RegisterResponse(
        UUID localId,
        UUID authId,
        String role
) {
    public static RegisterResponse from(RegisterUserResult result) {
        return new RegisterResponse(result.localId(), result.authId(), result.role().name());
    }
}
