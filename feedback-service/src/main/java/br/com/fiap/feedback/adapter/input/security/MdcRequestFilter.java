package br.com.fiap.feedback.adapter.input.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Populates the SLF4J MDC for each request so logs can be correlated.
 *
 * <p>Sets {@code correlationId} (from the {@code X-Correlation-Id} header or a generated
 * UUID) and, when a JWT is present, {@code authId}. The {@code role} is added later by
 * {@link CurrentUser} once resolved. The MDC is always cleared in the response filter to
 * avoid leaking context across pooled request threads.</p>
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class MdcRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String AUTH_ID_KEY = "authId";
    public static final String ROLE_KEY = "role";

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID_KEY, correlationId);

        // Echo the id back so the caller can correlate too.
        requestContext.getHeaders().putSingle(CORRELATION_ID_HEADER, correlationId);

        try {
            String authId = jwt.getClaim(CurrentUser.AUTH_ID_CLAIM);
            if (authId != null && !authId.isBlank()) {
                MDC.put(AUTH_ID_KEY, authId);
            }
        } catch (Exception ignored) {
            // No (valid) token on this request (e.g. public /cadastro) - nothing to add.
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId != null) {
            responseContext.getHeaders().putSingle(CORRELATION_ID_HEADER, correlationId);
        }
        MDC.remove(CORRELATION_ID_KEY);
        MDC.remove(AUTH_ID_KEY);
        MDC.remove(ROLE_KEY);
    }
}
