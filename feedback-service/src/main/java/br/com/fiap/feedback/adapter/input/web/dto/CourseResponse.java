package br.com.fiap.feedback.adapter.input.web.dto;

import br.com.fiap.feedback.domain.Course;

import java.util.UUID;

/** Representation of a course returned by the API. */
public record CourseResponse(
        UUID id,
        String name,
        String description
) {
    public static CourseResponse from(Course c) {
        return new CourseResponse(c.getId(), c.getName(), c.getDescription());
    }
}
