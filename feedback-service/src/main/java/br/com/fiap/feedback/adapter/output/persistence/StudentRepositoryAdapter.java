package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.StudentEntity;
import br.com.fiap.feedback.application.port.output.StudentRepositoryPort;
import br.com.fiap.feedback.domain.Student;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StudentRepositoryAdapter
        implements PanacheRepositoryBase<StudentEntity, UUID>, StudentRepositoryPort {

    @Override
    public Student save(Student student) {
        StudentEntity entity = PersistenceMapper.toEntity(student);
        persist(entity);
        return PersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<Student> findByLocalId(UUID id) {
        return findByIdOptional(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<Student> findByAuthId(UUID authId) {
        return find("authId", authId).firstResultOptional().map(PersistenceMapper::toDomain);
    }
}
