package br.com.fiap.feedback.domain;

/**
 * Application role of a registered user.
 *
 * <p>The role is <strong>not</strong> carried by the JWT issued by the auth-service
 * (that token has no {@code groups}/role claim), so it is resolved locally: a user
 * present in the {@code admin} table is an {@link #ADMIN}; otherwise a user in the
 * {@code student} table is a {@link #STUDENT}.</p>
 */
public enum Role {
    STUDENT,
    ADMIN
}
