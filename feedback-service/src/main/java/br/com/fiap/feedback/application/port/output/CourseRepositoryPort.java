package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Course;

import java.util.List;
import java.util.UUID;

public interface CourseRepositoryPort {

    Course save(Course course);

    List<Course> findAllCourses();

    boolean existsById(UUID id);
}
