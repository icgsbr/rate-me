package br.com.fiap.feedback.adapter.output.auth;

import br.com.fiap.feedback.adapter.output.auth.dto.AuthRegisterRequest;
import br.com.fiap.feedback.adapter.output.auth.dto.AuthRegisterResponse;
import br.com.fiap.feedback.domain.exception.RegistrationException;
import br.com.fiap.feedback.domain.exception.RegistrationRejectedException;
import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthClientAdapterTest {

    private static final UUID EXTERNAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private AuthClient authClient;

    @Captor
    private ArgumentCaptor<AuthRegisterRequest> requestCaptor;

    private AuthClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuthClientAdapter(authClient);
    }

    @Test
    @DisplayName("sends the local id as externalId and returns the auth-service id")
    void registersAndReturnsTheAuthId() {
        when(authClient.register(requestCaptor.capture()))
                .thenReturn(new AuthRegisterResponse(AUTH_ID.toString()));

        UUID result = adapter.register("alice", "s3cret", EXTERNAL_ID);

        assertThat(result).isEqualTo(AUTH_ID);

        AuthRegisterRequest sent = requestCaptor.getValue();
        assertThat(sent.login()).isEqualTo("alice");
        assertThat(sent.password()).isEqualTo("s3cret");
        assertThat(sent.externalId()).isEqualTo(EXTERNAL_ID.toString());
    }

    @Test
    @DisplayName("lets a rejection through unchanged so its status reaches the caller")
    void propagatesARejection() {
        when(authClient.register(any(AuthRegisterRequest.class)))
                .thenThrow(new RegistrationRejectedException(409, "login already taken"));

        assertThatThrownBy(() -> adapter.register("alice", "s3cret", EXTERNAL_ID))
                .isInstanceOf(RegistrationRejectedException.class)
                .hasMessage("login already taken")
                .extracting(e -> ((RegistrationRejectedException) e).getStatus())
                .isEqualTo(409);
    }

    @Test
    @DisplayName("lets an existing RegistrationException through without double wrapping")
    void propagatesAnExistingRegistrationException() {
        when(authClient.register(any(AuthRegisterRequest.class)))
                .thenThrow(new RegistrationException("already wrapped"));

        assertThatThrownBy(() -> adapter.register("alice", "s3cret", EXTERNAL_ID))
                .isInstanceOf(RegistrationException.class)
                .hasMessage("already wrapped")
                .hasNoCause();
    }

    @Test
    @DisplayName("wraps a transport failure as an infrastructure-level registration error")
    void wrapsATransportFailure() {
        when(authClient.register(any(AuthRegisterRequest.class)))
                .thenThrow(new ProcessingException("connection refused"));

        assertThatThrownBy(() -> adapter.register("alice", "s3cret", EXTERNAL_ID))
                .isInstanceOf(RegistrationException.class)
                .hasMessage("Could not register user in auth-service")
                .hasRootCauseMessage("connection refused");
    }

    @Test
    @DisplayName("wraps an unusable id in the auth-service response instead of leaking it")
    void wrapsAMalformedResponseId() {
        when(authClient.register(any(AuthRegisterRequest.class)))
                .thenReturn(new AuthRegisterResponse("not-a-uuid"));

        assertThatThrownBy(() -> adapter.register("alice", "s3cret", EXTERNAL_ID))
                .isInstanceOf(RegistrationException.class)
                .hasMessage("Could not register user in auth-service")
                .cause().isInstanceOf(IllegalArgumentException.class);

        verify(authClient).register(any(AuthRegisterRequest.class));
    }
}
