package br.com.fiap.feedback.adapter.input.web.error;

import br.com.fiap.feedback.domain.exception.CourseNotFoundException;
import br.com.fiap.feedback.domain.exception.ForbiddenOperationException;
import br.com.fiap.feedback.domain.exception.RegistrationException;
import br.com.fiap.feedback.domain.exception.RegistrationRejectedException;
import br.com.fiap.feedback.domain.exception.UserNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionMappersTest {

    @Test
    @DisplayName("an unknown local user becomes 404 not_found")
    void mapsUserNotFoundTo404() {
        Response response = new DomainExceptionMappers.UserNotFoundMapper()
                .toResponse(new UserNotFoundException("No local user for authId 42"));

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ErrorResponse("not_found", "No local user for authId 42"));
    }

    @Test
    @DisplayName("an unknown course becomes 404 not_found")
    void mapsCourseNotFoundTo404() {
        Response response = new DomainExceptionMappers.CourseNotFoundMapper()
                .toResponse(new CourseNotFoundException("Course not found: 7"));

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ErrorResponse("not_found", "Course not found: 7"));
    }

    @Test
    @DisplayName("a role violation becomes 403 forbidden")
    void mapsForbiddenOperationTo403() {
        Response response = new DomainExceptionMappers.ForbiddenMapper()
                .toResponse(new ForbiddenOperationException("Only admins can perform this action"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getEntity())
                .isEqualTo(new ErrorResponse("forbidden", "Only admins can perform this action"));
    }

    @Test
    @DisplayName("an invalid argument becomes 400 bad_request")
    void mapsIllegalArgumentTo400() {
        Response response = new DomainExceptionMappers.IllegalArgumentMapper()
                .toResponse(new IllegalArgumentException("score must be between 0 and 10"));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isEqualTo(new ErrorResponse("bad_request", "score must be between 0 and 10"));
    }

    @Test
    @DisplayName("an upstream auth-service failure becomes 502, not a 500")
    void mapsRegistrationFailureTo502() {
        Response response = new DomainExceptionMappers.RegistrationMapper()
                .toResponse(new RegistrationException("Could not register user in auth-service"));

        assertThat(response.getStatus()).isEqualTo(502);
        assertThat(response.getEntity()).isEqualTo(
                new ErrorResponse("registration_failed", "Could not register user in auth-service"));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 409, 422})
    @DisplayName("a rejection replays the status the auth-service returned")
    void replaysTheRejectionStatus(int status) {
        Response response = new DomainExceptionMappers.RegistrationRejectedMapper()
                .toResponse(new RegistrationRejectedException(status, "login already taken"));

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getEntity())
                .isEqualTo(new ErrorResponse("registration_rejected", "login already taken"));
    }
}
