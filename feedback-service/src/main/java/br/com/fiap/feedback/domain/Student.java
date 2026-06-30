package br.com.fiap.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * A student able to submit feedback for lessons.
 *
 * <p>Pure domain model (no persistence/JSON annotations). {@code authId} is the
 * identifier returned by the auth-service when the user is registered there; it is the
 * link used to resolve the caller from the validated JWT.</p>
 */
@Getter
@Builder
public class Student {

    /** Local identifier; also sent to the auth-service as {@code externalId}. */
    private final UUID id;
    private final String name;
    private final String registrationNumber;

    /** Identifier of the user inside the auth-service. */
    private final UUID authId;
}
