package br.com.fiap.feedback.adapter.input.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Course registration payload for {@code POST /courses}. */
public record CourseRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 1000) String description
) {}
