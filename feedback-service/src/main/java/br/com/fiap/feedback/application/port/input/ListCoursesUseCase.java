package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Course;

import java.util.List;

public interface ListCoursesUseCase {

    List<Course> listAll();
}
