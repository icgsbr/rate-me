package br.com.fiap.feedback.application.service;

import br.com.fiap.feedback.application.port.input.RegisterUserUseCase;
import br.com.fiap.feedback.application.port.output.AdminRepositoryPort;
import br.com.fiap.feedback.application.port.output.AuthClientPort;
import br.com.fiap.feedback.application.port.output.StudentRepositoryPort;
import br.com.fiap.feedback.domain.Admin;
import br.com.fiap.feedback.domain.Role;
import br.com.fiap.feedback.domain.Student;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ApplicationScoped
public class RegisterUserService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

    private final AuthClientPort authClient;
    private final StudentRepositoryPort studentRepository;
    private final AdminRepositoryPort adminRepository;

    public RegisterUserService(AuthClientPort authClient,
                               StudentRepositoryPort studentRepository,
                               AdminRepositoryPort adminRepository) {
        this.authClient = authClient;
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public RegisterUserResult register(RegisterUserCommand command) {
        UUID localId = UUID.randomUUID();
        Role role = command.role();

        log.info("Registering {} with login '{}' (localId={})", role, command.login(), localId);

        UUID authId = authClient.register(command.login(), command.password(), localId);

        if (role == Role.ADMIN) {
            adminRepository.save(Admin.builder()
                    .id(localId)
                    .name(command.name())
                    .authId(authId)
                    .role(Role.ADMIN.name())
                    .build());
        } else {
            studentRepository.save(Student.builder()
                    .id(localId)
                    .name(command.name())
                    .registrationNumber(command.registrationNumber())
                    .authId(authId)
                    .build());
        }

        log.info("User registered (localId={}, authId={}, role={})", localId, authId, role);
        return new RegisterUserResult(localId, authId, role);
    }
}
