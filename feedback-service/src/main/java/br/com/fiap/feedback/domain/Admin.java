package br.com.fiap.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * An administrator who consumes feedback reports and listings.
 *
 * <p>Pure domain model. As with {@link Student}, {@code authId} links the local record
 * to the auth-service user resolved from the JWT.</p>
 */
@Getter
@Builder
public class Admin {

    /** Local identifier; also sent to the auth-service as {@code externalId}. */
    private final UUID id;
    private final String name;

    /** Identifier of the user inside the auth-service. */
    private final UUID authId;

    /** Free-form administrative role label (e.g. "ADMIN"). */
    private final String role;
}
