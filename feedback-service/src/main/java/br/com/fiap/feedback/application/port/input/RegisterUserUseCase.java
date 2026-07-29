package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Role;

import java.util.UUID;

public interface RegisterUserUseCase {

    RegisterUserResult register(RegisterUserCommand command);

    record RegisterUserCommand(
            String name,
            String login,
            String password,
            String registrationNumber,
            Role role
    ) {}

    record RegisterUserResult(
            UUID localId,
            UUID authId,
            Role role
    ) {}
}
