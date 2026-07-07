package br.com.fiap.feedback.adapter.output.auth;

import br.com.fiap.feedback.adapter.output.auth.dto.AuthRegisterRequest;
import br.com.fiap.feedback.adapter.output.auth.dto.AuthRegisterResponse;
import br.com.fiap.feedback.application.port.output.AuthClientPort;
import br.com.fiap.feedback.domain.exception.RegistrationException;
import br.com.fiap.feedback.domain.exception.RegistrationRejectedException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Adapts the typed {@link AuthClient} to the application's {@link AuthClientPort}.
 * {@link RegistrationRejectedException} (auth-service rejected the data, e.g. invalid
 * password) is let through as-is; anything else (5xx, timeout, unreachable host) is
 * wrapped into a {@link RegistrationException}, an infrastructure failure.
 */
@ApplicationScoped
public class AuthClientAdapter implements AuthClientPort {

    private static final Logger log = LoggerFactory.getLogger(AuthClientAdapter.class);

    private final AuthClient authClient;

    public AuthClientAdapter(@RestClient AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public UUID register(String login, String password, UUID externalId) {
        try {
            AuthRegisterResponse response = authClient.register(
                    new AuthRegisterRequest(login, password, externalId.toString()));
            return UUID.fromString(response.id());
        } catch (RegistrationException | RegistrationRejectedException e) {
            throw e;
        } catch (Exception e) {
            log.error("auth-service registration failed for login '{}'", login, e);
            throw new RegistrationException("Could not register user in auth-service", e);
        }
    }
}
