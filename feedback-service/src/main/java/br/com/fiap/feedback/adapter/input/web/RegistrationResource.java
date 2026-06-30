package br.com.fiap.feedback.adapter.input.web;

import br.com.fiap.feedback.adapter.input.web.dto.RegisterRequest;
import br.com.fiap.feedback.adapter.input.web.dto.RegisterResponse;
import br.com.fiap.feedback.application.port.input.RegisterUserUseCase;
import br.com.fiap.feedback.application.port.input.RegisterUserUseCase.RegisterUserCommand;
import br.com.fiap.feedback.domain.Role;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Public registration endpoint. The route name ({@code /cadastro}) follows the agreed
 * platform contract; payloads and code are in English.
 */
@Path("/cadastro")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RegistrationResource {

    private final RegisterUserUseCase registerUser;

    public RegistrationResource(RegisterUserUseCase registerUser) {
        this.registerUser = registerUser;
    }

    @POST
    public Response register(@Valid RegisterRequest request) {
        Role role = parseRole(request.role());

        RegisterResponse response = RegisterResponse.from(
                registerUser.register(new RegisterUserCommand(
                        request.name(),
                        request.login(),
                        request.password(),
                        request.registrationNumber(),
                        role)));

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /** Defaults to STUDENT when the role is missing or unrecognised. */
    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.STUDENT;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Role.STUDENT;
        }
    }
}
