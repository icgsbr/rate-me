package br.com.fiap.feedback.application.port.output;

import java.util.UUID;

/** Output port for the external auth-service. */
public interface AuthClientPort {

    /**
     * Registers credentials in the auth-service.
     *
     * @param login       the user login
     * @param password    the raw password
     * @param externalId  our local user id, stored by the auth-service as externalId
     * @return the auth-service user id ({@code authId})
     */
    UUID register(String login, String password, UUID externalId);
}
