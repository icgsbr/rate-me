package br.com.fiap.feedback.adapter.output.auth;

import br.com.fiap.feedback.adapter.output.auth.dto.AuthRegisterRequest;
import br.com.fiap.feedback.adapter.output.auth.dto.AuthRegisterResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Typed REST client for the external auth-service. The base URL is configured via
 * {@code quarkus.rest-client.auth-service.url}. Client-error responses are translated by
 * {@link AuthProblemDetailMapper} into a domain-level rejection instead of a generic
 * transport exception.
 */
@RegisterRestClient(configKey = "auth-service")
@RegisterProvider(AuthProblemDetailMapper.class)
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AuthClient {

    @POST
    @Path("/register")
    AuthRegisterResponse register(AuthRegisterRequest request);
}
