package br.com.fiap.feedback.adapter.input.web;

import br.com.fiap.feedback.adapter.input.security.CurrentUser;
import br.com.fiap.feedback.adapter.input.web.dto.FeedbackRequest;
import br.com.fiap.feedback.adapter.input.web.dto.FeedbackResponse;
import br.com.fiap.feedback.adapter.input.web.dto.PagedFeedbackResponse;
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
    public Response submit(@Valid FeedbackRequest request) {
        var studentId = currentUser.requireStudent();

        Feedback feedback = submitFeedback.submit(
                new SubmitFeedbackCommand(studentId, request.description(), request.score()));

        return Response.status(Response.Status.CREATED)
                .entity(FeedbackResponse.from(feedback))
                .build();
    }

    @GET
    public Response list(@QueryParam("page") @DefaultValue("0") int page,
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
