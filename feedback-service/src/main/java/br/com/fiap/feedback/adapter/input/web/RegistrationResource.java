package br.com.fiap.feedback.adapter.input.web;

import br.com.fiap.feedback.adapter.input.web.dto.RegisterRequest;
import br.com.fiap.feedback.adapter.input.web.dto.RegisterResponse;
import br.com.fiap.feedback.adapter.input.web.error.ErrorResponse;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Public registration endpoint. The route name ({@code /cadastro}) follows the agreed
 * platform contract; payloads and code are in English.
 */
@Path("/cadastro")
@Tag(name = "Registration", description = "Public sign-up for students and admins")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RegistrationResource {

    private final RegisterUserUseCase registerUser;

    public RegistrationResource(RegisterUserUseCase registerUser) {
        this.registerUser = registerUser;
    }

    @POST
    @Operation(summary = "Register a new user",
            description = "Creates a local user (student or admin), registers its credentials in "
                    + "the auth-service and persists the returned authId locally. 'role' defaults "
                    + "to STUDENT when missing or unrecognised; 'registrationNumber' only applies "
                    + "to students. Not authenticated.")
    @RequestBody(content = @Content(schema = @Schema(implementation = RegisterRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "User registered",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload (blank name, login or password)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "502", description = "The upstream auth-service rejected the registration",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
