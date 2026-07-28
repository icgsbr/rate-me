package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Course;

import java.util.List;

/**
 * Use case for listing the registered courses, so a student can pick the one to
 * give feedback on.
 */
public interface ListCoursesUseCase {

    List<Course> listAll();
}
