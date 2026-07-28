package br.com.fiap.feedback.adapter.input.web;

import br.com.fiap.feedback.adapter.input.security.CurrentUser;
import br.com.fiap.feedback.adapter.input.web.dto.FeedbackRequest;
import br.com.fiap.feedback.adapter.input.web.dto.FeedbackResponse;
import br.com.fiap.feedback.adapter.input.web.dto.PagedFeedbackResponse;
import br.com.fiap.feedback.adapter.input.web.error.ErrorResponse;
import br.com.fiap.feedback.application.port.input.ListAllFeedbackUseCase;
import br.com.fiap.feedback.application.port.input.ListMyFeedbackUseCase;
import br.com.fiap.feedback.application.port.input.SubmitFeedbackUseCase;
import br.com.fiap.feedback.application.port.input.SubmitFeedbackUseCase.SubmitFeedbackCommand;
import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Role;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Feedback endpoints. All routes require a valid JWT ({@link Authenticated}); the
 * effective role is resolved locally via {@link CurrentUser}.
 *
 * <ul>
 *   <li>{@code POST /feedback} — students submit a rating.</li>
 *   <li>{@code GET /feedback} — students list their own; admins get a paginated list of all.</li>
 * </ul>
 */
@Path("/feedback")
@Tag(name = "Feedback", description = "Submission and listing of course feedback")
@SecurityRequirement(name = "bearerAuth")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FeedbackResource {

    private final SubmitFeedbackUseCase submitFeedback;
    private final ListMyFeedbackUseCase listMyFeedback;
    private final ListAllFeedbackUseCase listAllFeedback;
    private final CurrentUser currentUser;

    public FeedbackResource(SubmitFeedbackUseCase submitFeedback,
                            ListMyFeedbackUseCase listMyFeedback,
                            ListAllFeedbackUseCase listAllFeedback,
                            CurrentUser currentUser) {
        this.submitFeedback = submitFeedback;
        this.listMyFeedback = listMyFeedback;
        this.listAllFeedback = listAllFeedback;
        this.currentUser = currentUser;
    }

    @POST
    @Operation(summary = "Submit feedback",
            description = "Students submit a rating (0-10) and description for a specific course; "
                    + "the courseId comes from GET /courses. A critical score triggers a low-score "
                    + "alert e-mail to the admin. Restricted to authenticated students.")
    @RequestBody(content = @Content(schema = @Schema(implementation = FeedbackRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Feedback created",
                    content = @Content(schema = @Schema(implementation = FeedbackResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload (blank description, missing courseId or score out of range)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @APIResponse(responseCode = "403", description = "Caller is not a student",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "The referenced course does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response submit(@Valid FeedbackRequest request) {
        var studentId = currentUser.requireStudent();

        Feedback feedback = submitFeedback.submit(new SubmitFeedbackCommand(
                studentId, request.description(), request.score(), request.courseId()));

        return Response.status(Response.Status.CREATED)
                .entity(FeedbackResponse.from(feedback))
                .build();
    }

    @GET
    @Operation(summary = "List feedback",
            description = "Students receive their own feedback as a plain list; admins receive a "
                    + "paginated list of every feedback (the page/size parameters are ignored for students).")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Own feedback (students) or a feedback page (admins)",
                    content = @Content(schema = @Schema(implementation = PagedFeedbackResponse.class))),
            @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public Response list(
            @Parameter(description = "Zero-based page index (admins only)")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size (admins only)")
            @QueryParam("size") @DefaultValue("20") int size) {
        if (currentUser.role() == Role.ADMIN) {
            return Response.ok(
                    PagedFeedbackResponse.from(listAllFeedback.listAll(page, size))).build();
        }

        List<FeedbackResponse> own = listMyFeedback.listForStudent(currentUser.localId())
                .stream()
                .map(FeedbackResponse::from)
                .toList();
        return Response.ok(own).build();
    }
}
