package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Student;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepositoryPort {

    Student save(Student student);

    Optional<Student> findByLocalId(UUID id);

    Optional<Student> findByAuthId(UUID authId);
}
