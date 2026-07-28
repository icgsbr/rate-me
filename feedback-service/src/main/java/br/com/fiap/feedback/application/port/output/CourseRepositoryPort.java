package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Course;

import java.util.List;
import java.util.UUID;

/** Output port for persisting and retrieving courses. */
public interface CourseRepositoryPort {

    Course save(Course course);

    /**
     * Named {@code findAllCourses} rather than {@code findAll}/{@code listAll} because
     * Panache already defines both on the adapter with incompatible return types — the
     * same reason {@link FeedbackRepositoryPort#countAll()} is not called {@code count}.
     */
    List<Course> findAllCourses();

    boolean existsById(UUID id);
}
