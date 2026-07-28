package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Course;

public interface CreateCourseUseCase {

    Course create(CreateCourseCommand command);

    record CreateCourseCommand(
            String name,
            String description
    ) {}
}
