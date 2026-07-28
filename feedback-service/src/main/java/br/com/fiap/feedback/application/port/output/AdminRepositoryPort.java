package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Admin;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepositoryPort {

    Admin save(Admin admin);

    Optional<Admin> findByAuthId(UUID authId);
}
