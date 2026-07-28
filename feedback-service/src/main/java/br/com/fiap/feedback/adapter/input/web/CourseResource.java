package br.com.fiap.feedback.adapter.input.web;

import br.com.fiap.feedback.adapter.input.security.CurrentUser;
import br.com.fiap.feedback.adapter.input.web.dto.CourseRequest;
import br.com.fiap.feedback.adapter.input.web.dto.CourseResponse;
import br.com.fiap.feedback.adapter.input.web.error.ErrorResponse;
import br.com.fiap.feedback.application.port.input.CreateCourseUseCase;
import br.com.fiap.feedback.application.port.input.CreateCourseUseCase.CreateCourseCommand;
import br.com.fiap.feedback.application.port.input.ListCoursesUseCase;
import br.com.fiap.feedback.domain.Course;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Course endpoints. All routes require a valid JWT ({@link Authenticated}); the effective
 * role is resolved locally via {@link CurrentUser}.
 *
 * <ul>
 *   <li>{@code POST /courses} — admins register a course.</li>
 *   <li>{@code GET /courses} — any authenticated caller lists them, so a student can pick
 *       the {@code courseId} to submit feedback for.</li>
 * </ul>
 */
@Path("/courses")
@Tag(name = "Courses", description = "Registration and listing of courses")
@SecurityRequirement(name = "bearerAuth")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CourseResource {

    private final CreateCourseUseCase createCourse;
    private final ListCoursesUseCase listCourses;
    private final CurrentUser currentUser;

    public CourseResource(CreateCourseUseCase createCourse,
                          ListCoursesUseCase listCourses,
                          CurrentUser currentUser) {
        this.createCourse = createCourse;
        this.listCourses = listCourses;
        this.currentUser = currentUser;
    }

    @POST
    @Operation(summary = "Register a course",
            description = "Creates a course with a name and a description. Restricted to admins.")
    @RequestBody(content = @Content(schema = @Schema(implementation = CourseRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Course created",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid payload (blank name or description)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @APIResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response create(@Valid CourseRequest request) {
        currentUser.requireAdmin();

        Course course = createCourse.create(
                new CreateCourseCommand(request.name(), request.description()));

        return Response.status(Response.Status.CREATED)
                .entity(CourseResponse.from(course))
                .build();
    }

    @GET
    @Operation(summary = "List courses",
            description = "Returns every registered course with its id, name and description. "
                    + "Open to any authenticated caller, since students need the id to submit feedback.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "The registered courses",
                    content = @Content(schema = @Schema(implementation = CourseResponse.class))),
            @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public Response list() {
        List<CourseResponse> courses = listCourses.listAll()
                .stream()
                .map(CourseResponse::from)
                .toList();
        return Response.ok(courses).build();
    }
}
