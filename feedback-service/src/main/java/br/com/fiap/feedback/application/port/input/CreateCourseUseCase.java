package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Course;

/**
 * Use case for registering a new course. Restricted to admins by the web adapter.
 */
public interface CreateCourseUseCase {

    Course create(CreateCourseCommand command);

    /**
     * @param name        short course title
     * @param description what the course covers
     */
    record CreateCourseCommand(
            String name,
            String description
    ) {}
}
