package br.com.fiap.feedback.adapter.output.auth;

import br.com.fiap.feedback.adapter.output.auth.dto.AuthProblemDetail;
import br.com.fiap.feedback.domain.exception.RegistrationRejectedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Translates a client-error ({@code 4xx}) response from the auth-service - its
 * {@code application/problem+json} body (invalid password, duplicate login, ...) - into a
 * {@link RegistrationRejectedException} carrying the real status and {@code detail} message.
 *
 * <p>Returning {@code null} (unparsable body, or a status this mapper doesn't handle) falls
 * back to the default {@link jakarta.ws.rs.WebApplicationException}, which
 * {@link AuthClientAdapter} then wraps as an infrastructure failure
 * ({@code RegistrationException}, HTTP 502).</p>
 */
public class AuthProblemDetailMapper implements ResponseExceptionMapper<RuntimeException> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public RuntimeException toThrowable(Response response) {
        try {
            String body = response.readEntity(String.class);
            AuthProblemDetail problem = MAPPER.readValue(body, AuthProblemDetail.class);
            String message = problem.detail() != null ? problem.detail() : problem.title();
            if (message != null && !message.isBlank()) {
                return new RegistrationRejectedException(response.getStatus(), message);
            }
        } catch (Exception ignored) {
            // Body isn't a problem+json we understand - fall back to the default mapping.
        }
        return null;
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400 && status < 500;
    }
}
