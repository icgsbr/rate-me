package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Student;

import java.util.Optional;
import java.util.UUID;

/** Output port for persisting and retrieving students. */
public interface StudentRepositoryPort {

    Student save(Student student);

    Optional<Student> findByLocalId(UUID id);

    /** Resolve a student by the auth-service identifier carried in the JWT. */
    Optional<Student> findByAuthId(UUID authId);
}
