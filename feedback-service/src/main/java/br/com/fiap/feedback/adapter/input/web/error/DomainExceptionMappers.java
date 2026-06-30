package br.com.fiap.feedback.adapter.input.web.error;

import br.com.fiap.feedback.domain.exception.ForbiddenOperationException;
import br.com.fiap.feedback.domain.exception.RegistrationException;
import br.com.fiap.feedback.domain.exception.UserNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps domain exceptions to HTTP responses. Grouped here (one mapper class per exception)
 * so the translation rules are easy to find.
 *
 * <p>Bean-validation failures ({@code ConstraintViolationException}) are handled by
 * Quarkus' built-in mapper, which already returns {@code 400}.</p>
 */
public final class DomainExceptionMappers {

    private static final Logger log = LoggerFactory.getLogger(DomainExceptionMappers.class);

    private DomainExceptionMappers() {
    }

    @Provider
    public static class UserNotFoundMapper implements ExceptionMapper<UserNotFoundException> {
        @Override
        public Response toResponse(UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("not_found", e.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class ForbiddenMapper implements ExceptionMapper<ForbiddenOperationException> {
        @Override
        public Response toResponse(ForbiddenOperationException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse("forbidden", e.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class IllegalArgumentMapper implements ExceptionMapper<IllegalArgumentException> {
        @Override
        public Response toResponse(IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("bad_request", e.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class RegistrationMapper implements ExceptionMapper<RegistrationException> {
        @Override
        public Response toResponse(RegistrationException e) {
            // The failure originates in the upstream auth-service.
            log.error("Registration failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse("registration_failed", e.getMessage()))
                    .build();
        }
    }
}
