package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.CourseEntity;
import br.com.fiap.feedback.application.port.output.CourseRepositoryPort;
import br.com.fiap.feedback.domain.Course;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Panache-backed implementation of {@link CourseRepositoryPort}, following the same shape
 * as {@link StudentRepositoryAdapter}.
 */
@ApplicationScoped
public class CourseRepositoryAdapter
        implements PanacheRepositoryBase<CourseEntity, UUID>, CourseRepositoryPort {

    @Override
    public Course save(Course course) {
        CourseEntity entity = PersistenceMapper.toEntity(course);
        persist(entity);
        return PersistenceMapper.toDomain(entity);
    }

    @Override
    public List<Course> findAllCourses() {
        return listAll().stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return count("id", id) > 0;
    }
}
