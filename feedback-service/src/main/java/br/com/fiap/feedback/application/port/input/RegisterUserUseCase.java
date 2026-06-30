package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Role;

import java.util.UUID;

/**
 * Use case for registering a new student or admin.
 *
 * <p>Registration generates a local identifier, registers the credentials in the
 * external auth-service (passing the local id as {@code externalId}) and persists the
 * user locally together with the returned {@code authId}.</p>
 */
public interface RegisterUserUseCase {

    RegisterUserResult register(RegisterUserCommand command);

    /** Input data for a registration request. */
    record RegisterUserCommand(
            String name,
            String login,
            String password,
            String registrationNumber,
            Role role
    ) {}

    /** Outcome of a successful registration. */
    record RegisterUserResult(
            UUID localId,
            UUID authId,
            Role role
    ) {}
}
