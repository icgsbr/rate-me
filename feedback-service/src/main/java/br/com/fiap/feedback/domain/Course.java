package br.com.fiap.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * A course/lesson that feedback is attached to.
 *
 * <p>Courses are registered by admins and listed by any authenticated caller, so a student
 * can pick one before submitting. Every {@link Feedback} references exactly one course.</p>
 */
@Getter
@Builder
public class Course {

    private final UUID id;
    private final String name;
    private final String description;
}
