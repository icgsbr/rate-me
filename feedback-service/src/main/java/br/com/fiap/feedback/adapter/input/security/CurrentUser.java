package br.com.fiap.feedback.adapter.input.security;

import br.com.fiap.feedback.application.port.output.AdminRepositoryPort;
import br.com.fiap.feedback.application.port.output.StudentRepositoryPort;
import br.com.fiap.feedback.domain.Role;
import br.com.fiap.feedback.domain.exception.ForbiddenOperationException;
import br.com.fiap.feedback.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.RequestScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

@RequestScoped
public class CurrentUser {

    static final String AUTH_ID_CLAIM = "authId";

    private final JsonWebToken jwt;
    private final AdminRepositoryPort adminRepository;
    private final StudentRepositoryPort studentRepository;

    private boolean resolved;
    private UUID authId;
    private UUID localId;
    private Role role;

    public CurrentUser(JsonWebToken jwt,
                       AdminRepositoryPort adminRepository,
                       StudentRepositoryPort studentRepository) {
        this.jwt = jwt;
        this.adminRepository = adminRepository;
        this.studentRepository = studentRepository;
    }

    public UUID authId() {
        resolve();
        return authId;
    }

    public UUID localId() {
        resolve();
        return localId;
    }

    public Role role() {
        resolve();
        return role;
    }

    public UUID requireStudent() {
        resolve();
        if (role != Role.STUDENT) {
            throw new ForbiddenOperationException("Only students can perform this action");
        }
        return localId;
    }

    public void requireAdmin() {
        resolve();
        if (role != Role.ADMIN) {
            throw new ForbiddenOperationException("Only admins can perform this action");
        }
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        String claim = jwt.getClaim(AUTH_ID_CLAIM);
        if (claim == null || claim.isBlank()) {
            throw new UserNotFoundException("Token is missing the '" + AUTH_ID_CLAIM + "' claim");
        }
        this.authId = UUID.fromString(claim);

        Optional<Role> resolvedRole = adminRepository.findByAuthId(authId)
                .map(admin -> {
                    this.localId = admin.getId();
                    return Role.ADMIN;
                })
                .or(() -> studentRepository.findByAuthId(authId)
                        .map(student -> {
                            this.localId = student.getId();
                            return Role.STUDENT;
                        }));

        this.role = resolvedRole.orElseThrow(() ->
                new UserNotFoundException("No local user for authId " + authId));

        MDC.put("role", role.name());
        this.resolved = true;
    }
}
