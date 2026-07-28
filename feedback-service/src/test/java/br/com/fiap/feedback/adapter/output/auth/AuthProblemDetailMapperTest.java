package br.com.fiap.feedback.adapter.output.auth;

import br.com.fiap.feedback.domain.exception.RegistrationRejectedException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthProblemDetailMapperTest {

    private final AuthProblemDetailMapper mapper = new AuthProblemDetailMapper();

    @Mock
    private Response response;

    @Test
    @DisplayName("prefers the problem detail and replays the upstream status")
    void usesTheDetailField() {
        stubBody(422, """
                {"type":"about:blank","title":"Unprocessable Entity","status":422,
                 "detail":"password must be at least 8 characters"}""");

        RuntimeException thrown = mapper.toThrowable(response);

        assertThat(thrown).isInstanceOf(RegistrationRejectedException.class)
                .hasMessage("password must be at least 8 characters");
        assertThat(((RegistrationRejectedException) thrown).getStatus()).isEqualTo(422);
    }

    @Test
    @DisplayName("falls back to the title when there is no detail")
    void fallsBackToTheTitle() {
        stubBody(409, """
                {"title":"Conflict","status":409}""");

        assertThat(mapper.toThrowable(response))
                .isInstanceOf(RegistrationRejectedException.class)
                .hasMessage("Conflict");
    }

    @Test
    @DisplayName("ignores unknown problem+json properties")
    void ignoresUnknownProperties() {
        stubBody(400, """
                {"detail":"invalid login","traceId":"abc","extra":{"nested":true}}""");

        assertThat(mapper.toThrowable(response))
                .isInstanceOf(RegistrationRejectedException.class)
                .hasMessage("invalid login");
    }

    @Test
    @DisplayName("gives up on a body with no usable message, so the default mapping applies")
    void returnsNullWithoutAMessage() {
        // The status is never read on this path, so it is deliberately not stubbed.
        when(response.readEntity(String.class)).thenReturn("{\"status\":400}");

        assertThat(mapper.toThrowable(response)).isNull();
    }

    @Test
    @DisplayName("gives up on a blank message")
    void returnsNullForABlankMessage() {
        when(response.readEntity(String.class)).thenReturn("{\"detail\":\"   \"}");

        assertThat(mapper.toThrowable(response)).isNull();
    }

    @Test
    @DisplayName("gives up when the body is not problem+json at all")
    void returnsNullForAnUnparseableBody() {
        when(response.readEntity(String.class)).thenReturn("<html>Bad Request</html>");

        assertThat(mapper.toThrowable(response)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "399, false",
            "400, true",
            "404, true",
            "499, true",
            "500, false",
            "503, false"
    })
    @DisplayName("handles client errors only, leaving 5xx to the infrastructure path")
    void handlesClientErrorsOnly(int status, boolean expected) {
        assertThat(mapper.handles(status, null)).isEqualTo(expected);
    }

    private void stubBody(int status, String body) {
        when(response.readEntity(String.class)).thenReturn(body);
        when(response.getStatus()).thenReturn(status);
    }
}
