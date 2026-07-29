package br.com.fiap.feedback.adapter.input.web;

import br.com.fiap.feedback.adapter.input.security.CurrentUser;
import br.com.fiap.feedback.adapter.input.web.dto.CourseRequest;
import br.com.fiap.feedback.adapter.input.web.dto.CourseResponse;
import br.com.fiap.feedback.application.port.input.CreateCourseUseCase;
import br.com.fiap.feedback.application.port.input.CreateCourseUseCase.CreateCourseCommand;
import br.com.fiap.feedback.application.port.input.ListCoursesUseCase;
import br.com.fiap.feedback.domain.Course;
import br.com.fiap.feedback.domain.exception.ForbiddenOperationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseResourceTest {

    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final Course COURSE = Course.builder()
            .id(COURSE_ID)
            .name("Arquitetura")
            .description("Padroes e trade-offs")
            .build();

    @Mock
    private CreateCourseUseCase createCourse;

    @Mock
    private ListCoursesUseCase listCourses;

    @Mock
    private CurrentUser currentUser;

    @Captor
    private ArgumentCaptor<CreateCourseCommand> commandCaptor;

    private CourseResource resource;

    @BeforeEach
    void setUp() {
        resource = new CourseResource(createCourse, listCourses, currentUser);
    }

    @Test
    @DisplayName("answers 201 with the created course when an admin registers one")
    void createReturnsCreated() {
        when(createCourse.create(commandCaptor.capture())).thenReturn(COURSE);

        Response response = resource.create(new CourseRequest("Arquitetura", "Padroes e trade-offs"));

        assertThat(response.getStatus()).isEqualTo(201);
        CourseResponse body = (CourseResponse) response.getEntity();
        assertThat(body.id()).isEqualTo(COURSE_ID);
        assertThat(body.name()).isEqualTo("Arquitetura");
        assertThat(body.description()).isEqualTo("Padroes e trade-offs");

        assertThat(commandCaptor.getValue().name()).isEqualTo("Arquitetura");
        verify(currentUser).requireAdmin();
    }

    @Test
    @DisplayName("refuses course registration from a non-admin before reaching the use case")
    void createRejectsNonAdmins() {
        doThrow(new ForbiddenOperationException("Only admins can perform this action"))
                .when(currentUser).requireAdmin();

        assertThatThrownBy(() -> resource.create(new CourseRequest("Arquitetura", "Padroes")))
                .isInstanceOf(ForbiddenOperationException.class);

        verifyNoInteractions(createCourse);
    }

    @Test
    @DisplayName("lists courses for any authenticated caller, without a role check")
    void listIsOpenToAnyAuthenticatedCaller() {
        when(listCourses.listAll()).thenReturn(List.of(COURSE));

        Response response = resource.list();

        assertThat(response.getStatus()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        List<CourseResponse> body = (List<CourseResponse>) response.getEntity();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).id()).isEqualTo(COURSE_ID);

        verifyNoInteractions(currentUser);
    }

    @Test
    @DisplayName("returns an empty list when no course is registered yet")
    void listReturnsEmptyWhenThereAreNoCourses() {
        when(listCourses.listAll()).thenReturn(List.of());

        assertThat((List<?>) resource.list().getEntity()).isEmpty();
    }
}
