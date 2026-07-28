package br.com.fiap.feedback.application.service;

import br.com.fiap.feedback.application.port.input.RegisterUserUseCase.RegisterUserCommand;
import br.com.fiap.feedback.application.port.input.RegisterUserUseCase.RegisterUserResult;
import br.com.fiap.feedback.application.port.output.AdminRepositoryPort;
import br.com.fiap.feedback.application.port.output.AuthClientPort;
import br.com.fiap.feedback.application.port.output.StudentRepositoryPort;
import br.com.fiap.feedback.domain.Admin;
import br.com.fiap.feedback.domain.Role;
import br.com.fiap.feedback.domain.Student;
import br.com.fiap.feedback.domain.exception.RegistrationException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    private static final UUID AUTH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private AuthClientPort authClient;

    @Mock
    private StudentRepositoryPort studentRepository;

    @Mock
    private AdminRepositoryPort adminRepository;

    @Captor
    private ArgumentCaptor<UUID> localIdCaptor;

    @Captor
    private ArgumentCaptor<Student> studentCaptor;

    @Captor
    private ArgumentCaptor<Admin> adminCaptor;

    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(authClient, studentRepository, adminRepository);
    }

    @Test
    @DisplayName("registers a student locally under the same id sent to the auth-service")
    void registersAStudent() {
        when(authClient.register(eq("alice"), eq("s3cret"), any(UUID.class))).thenReturn(AUTH_ID);

        RegisterUserResult result = service.register(
                new RegisterUserCommand("Alice", "alice", "s3cret", "RM12345", Role.STUDENT));

        verify(authClient).register(eq("alice"), eq("s3cret"), localIdCaptor.capture());
        verify(studentRepository).save(studentCaptor.capture());

        Student saved = studentCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(localIdCaptor.getValue());
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getRegistrationNumber()).isEqualTo("RM12345");
        assertThat(saved.getAuthId()).isEqualTo(AUTH_ID);

        assertThat(result.localId()).isEqualTo(localIdCaptor.getValue());
        assertThat(result.authId()).isEqualTo(AUTH_ID);
        assertThat(result.role()).isEqualTo(Role.STUDENT);

        verifyNoInteractions(adminRepository);
    }

    @Test
    @DisplayName("registers an admin in the admin table with the ADMIN role label")
    void registersAnAdmin() {
        when(authClient.register(eq("root"), eq("s3cret"), any(UUID.class))).thenReturn(AUTH_ID);

        RegisterUserResult result = service.register(
                new RegisterUserCommand("Root", "root", "s3cret", null, Role.ADMIN));

        verify(adminRepository).save(adminCaptor.capture());

        Admin saved = adminCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Root");
        assertThat(saved.getAuthId()).isEqualTo(AUTH_ID);
        assertThat(saved.getRole()).isEqualTo("ADMIN");

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verifyNoInteractions(studentRepository);
    }

    @Test
    @DisplayName("generates a fresh local id for every registration")
    void generatesAFreshLocalIdEachTime() {
        when(authClient.register(any(), any(), any(UUID.class))).thenReturn(AUTH_ID);
        RegisterUserCommand command =
                new RegisterUserCommand("Alice", "alice", "s3cret", "RM1", Role.STUDENT);

        UUID first = service.register(command).localId();
        UUID second = service.register(command).localId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("leaves no local user behind when the auth-service refuses the registration")
    void doesNotPersistLocallyWhenAuthFails() {
        when(authClient.register(any(), any(), any(UUID.class)))
                .thenThrow(new RegistrationException("auth-service unreachable"));

        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand("Alice", "alice", "s3cret", "RM1", Role.STUDENT)))
                .isInstanceOf(RegistrationException.class)
                .hasMessage("auth-service unreachable");

        verifyNoInteractions(studentRepository, adminRepository);
    }
}
