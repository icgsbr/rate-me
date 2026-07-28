package br.com.fiap.feedback.adapter.input.security;

import br.com.fiap.feedback.application.port.output.AdminRepositoryPort;
import br.com.fiap.feedback.application.port.output.StudentRepositoryPort;
import br.com.fiap.feedback.domain.Admin;
import br.com.fiap.feedback.domain.Role;
import br.com.fiap.feedback.domain.Student;
import br.com.fiap.feedback.domain.exception.ForbiddenOperationException;
import br.com.fiap.feedback.domain.exception.UserNotFoundException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserTest {

    private static final UUID AUTH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID LOCAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Admin ADMIN = Admin.builder()
            .id(LOCAL_ID).name("Root").authId(AUTH_ID).role("ADMIN").build();
    private static final Student STUDENT = Student.builder()
            .id(LOCAL_ID).name("Alice").registrationNumber("RM1").authId(AUTH_ID).build();

    @Mock
    private JsonWebToken jwt;

    @Mock
    private AdminRepositoryPort adminRepository;

    @Mock
    private StudentRepositoryPort studentRepository;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("refuses a token without a usable authId claim")
    void rejectsAMissingClaim(String claim) {
        when(jwt.<String>getClaim(CurrentUser.AUTH_ID_CLAIM)).thenReturn(claim);

        assertThatThrownBy(() -> currentUser().authId())
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Token is missing the 'authId' claim");

        verifyNoInteractions(adminRepository, studentRepository);
    }

    @Test
    @DisplayName("surfaces a malformed authId claim as an invalid argument (mapped to 400)")
    void rejectsAMalformedClaim() {
        when(jwt.<String>getClaim(CurrentUser.AUTH_ID_CLAIM)).thenReturn("not-a-uuid");

        assertThatThrownBy(() -> currentUser().authId())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("resolves an admin without falling through to the student table")
    void resolvesAnAdmin() {
        stubClaim();
        when(adminRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(ADMIN));

        CurrentUser currentUser = currentUser();

        assertThat(currentUser.role()).isEqualTo(Role.ADMIN);
        assertThat(currentUser.localId()).isEqualTo(LOCAL_ID);
        assertThat(currentUser.authId()).isEqualTo(AUTH_ID);
        assertThat(MDC.get("role")).isEqualTo("ADMIN");

        verifyNoInteractions(studentRepository);
    }

    @Test
    @DisplayName("falls back to the student table when the caller is not an admin")
    void resolvesAStudent() {
        stubClaim();
        when(adminRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.empty());
        when(studentRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(STUDENT));

        CurrentUser currentUser = currentUser();

        assertThat(currentUser.role()).isEqualTo(Role.STUDENT);
        assertThat(currentUser.localId()).isEqualTo(LOCAL_ID);
        assertThat(MDC.get("role")).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("refuses a valid token that has no local user behind it")
    void rejectsAnUnknownLocalUser() {
        stubClaim();
        when(adminRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.empty());
        when(studentRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUser().role())
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("No local user for authId " + AUTH_ID);
    }

    @Test
    @DisplayName("resolves once and reuses the result across accessors")
    void memoisesTheResolution() {
        stubClaim();
        when(adminRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(ADMIN));

        CurrentUser currentUser = currentUser();
        currentUser.authId();
        currentUser.role();
        currentUser.localId();
        currentUser.requireAdmin();

        verify(adminRepository, times(1)).findByAuthId(AUTH_ID);
        verify(jwt, times(1)).getClaim(CurrentUser.AUTH_ID_CLAIM);
    }

    @Test
    @DisplayName("requireStudent returns the local id for a student and rejects an admin")
    void requireStudentEnforcesTheRole() {
        stubClaim();
        when(adminRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.empty());
        when(studentRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(STUDENT));

        assertThat(currentUser().requireStudent()).isEqualTo(LOCAL_ID);
    }

    @Test
    @DisplayName("requireStudent rejects an admin")
    void requireStudentRejectsAnAdmin() {
        stubClaim();
        when(adminRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(ADMIN));

        assertThatThrownBy(() -> currentUser().requireStudent())
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only students can perform this action");
    }

    @Test
    @DisplayName("requireAdmin accepts an admin and rejects a student")
    void requireAdminRejectsAStudent() {
        stubClaim();
        when(adminRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.empty());
        when(studentRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(STUDENT));

        assertThatThrownBy(() -> currentUser().requireAdmin())
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only admins can perform this action");
    }

    private void stubClaim() {
        when(jwt.<String>getClaim(CurrentUser.AUTH_ID_CLAIM)).thenReturn(AUTH_ID.toString());
    }

    private CurrentUser currentUser() {
        return new CurrentUser(jwt, adminRepository, studentRepository);
    }
}
