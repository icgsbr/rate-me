package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Admin;

import java.util.Optional;
import java.util.UUID;

/** Output port for persisting and retrieving admins. */
public interface AdminRepositoryPort {

    Admin save(Admin admin);

    /** Resolve an admin by the auth-service identifier carried in the JWT. */
    Optional<Admin> findByAuthId(UUID authId);
}
