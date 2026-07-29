package br.com.fiap.feedback.adapter.input.web;

import br.com.fiap.feedback.adapter.input.web.dto.RegisterRequest;
import br.com.fiap.feedback.adapter.input.web.dto.RegisterResponse;
import br.com.fiap.feedback.application.port.input.RegisterUserUseCase;
import br.com.fiap.feedback.application.port.input.RegisterUserUseCase.RegisterUserCommand;
import br.com.fiap.feedback.application.port.input.RegisterUserUseCase.RegisterUserResult;
import br.com.fiap.feedback.domain.Role;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationResourceTest {

    private static final UUID LOCAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private RegisterUserUseCase registerUser;

    @Captor
    private ArgumentCaptor<RegisterUserCommand> commandCaptor;

    private RegistrationResource resource;

    @BeforeEach
    void setUp() {
        resource = new RegistrationResource(registerUser);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "admin", "  Admin  "})
    @DisplayName("recognises the admin role regardless of case and padding")
    void parsesTheAdminRole(String role) {
        stubRegistration(Role.ADMIN);

        resource.register(request(role));

        assertThat(commandCaptor.getValue().role()).isEqualTo(Role.ADMIN);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "STUDENT", "student", "bogus", "root"})
    @DisplayName("falls back to STUDENT when the role is missing, blank or unrecognised")
    void defaultsToStudent(String role) {
        stubRegistration(Role.STUDENT);

        resource.register(request(role));

        assertThat(commandCaptor.getValue().role()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("answers 201 with the local and auth identifiers")
    void returnsCreatedWithBothIdentifiers() {
        stubRegistration(Role.STUDENT);

        Response response = resource.register(request("student"));

        assertThat(response.getStatus()).isEqualTo(201);
        RegisterResponse body = (RegisterResponse) response.getEntity();
        assertThat(body.localId()).isEqualTo(LOCAL_ID);
        assertThat(body.authId()).isEqualTo(AUTH_ID);
        assertThat(body.role()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("forwards the whole payload to the use case")
    void forwardsThePayload() {
        stubRegistration(Role.STUDENT);

        resource.register(new RegisterRequest("Alice", "alice", "s3cret", "RM12345", "student"));

        RegisterUserCommand command = commandCaptor.getValue();
        assertThat(command.name()).isEqualTo("Alice");
        assertThat(command.login()).isEqualTo("alice");
        assertThat(command.password()).isEqualTo("s3cret");
        assertThat(command.registrationNumber()).isEqualTo("RM12345");
    }

    private void stubRegistration(Role role) {
        when(registerUser.register(commandCaptor.capture()))
                .thenReturn(new RegisterUserResult(LOCAL_ID, AUTH_ID, role));
    }

    private static RegisterRequest request(String role) {
        return new RegisterRequest("Alice", "alice", "s3cret", "RM12345", role);
    }
}
