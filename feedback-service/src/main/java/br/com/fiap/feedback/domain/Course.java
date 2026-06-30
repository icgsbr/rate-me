package br.com.fiap.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * A course/lesson that feedback can be attached to.
 *
 * <p>The public feedback contract only carries {@code description} and {@code score},
 * so submissions are attached to a single seeded default course (see Flyway
 * {@code V1__init_schema.sql}). The entity is kept to preserve the relational model and
 * to leave room for per-course feedback later.</p>
 */
@Getter
@Builder
public class Course {

    private final UUID id;
    private final String name;
}
