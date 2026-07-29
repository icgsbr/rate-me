package br.com.fiap.feedback.adapter.input.web;

import br.com.fiap.feedback.adapter.input.security.CurrentUser;
import br.com.fiap.feedback.adapter.input.web.dto.FeedbackRequest;
import br.com.fiap.feedback.adapter.input.web.dto.FeedbackResponse;
import br.com.fiap.feedback.adapter.input.web.dto.PagedFeedbackResponse;
import br.com.fiap.feedback.application.port.input.ListAllFeedbackUseCase;
import br.com.fiap.feedback.application.port.input.ListAllFeedbackUseCase.FeedbackPage;
import br.com.fiap.feedback.application.port.input.ListMyFeedbackUseCase;
import br.com.fiap.feedback.application.port.input.SubmitFeedbackUseCase;
import br.com.fiap.feedback.application.port.input.SubmitFeedbackUseCase.SubmitFeedbackCommand;
import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Role;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackResourceTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final Feedback FEEDBACK = Feedback.builder()
            .id(7L)
            .studentId(STUDENT_ID)
            .courseId(COURSE_ID)
            .score(8)
            .reviewDescription("great course")
            .reviewDate(OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC))
            .notified(false)
            .build();

    @Mock
    private SubmitFeedbackUseCase submitFeedback;

    @Mock
    private ListMyFeedbackUseCase listMyFeedback;

    @Mock
    private ListAllFeedbackUseCase listAllFeedback;

    @Mock
    private CurrentUser currentUser;

    @Captor
    private ArgumentCaptor<SubmitFeedbackCommand> commandCaptor;

    private FeedbackResource resource;

    @BeforeEach
    void setUp() {
        resource = new FeedbackResource(submitFeedback, listMyFeedback, listAllFeedback, currentUser);
    }

    @Test
    @DisplayName("answers 201 and echoes the created feedback back to the student")
    void submitReturnsCreated() {
        when(currentUser.requireStudent()).thenReturn(STUDENT_ID);
        when(submitFeedback.submit(commandCaptor.capture())).thenReturn(FEEDBACK);

        Response response = resource.submit(new FeedbackRequest("great course", 8, COURSE_ID));

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getEntity()).isInstanceOf(FeedbackResponse.class);

        FeedbackResponse body = (FeedbackResponse) response.getEntity();
        assertThat(body.id()).isEqualTo(7L);
        assertThat(body.score()).isEqualTo(8);

        SubmitFeedbackCommand command = commandCaptor.getValue();
        assertThat(command.studentId()).isEqualTo(STUDENT_ID);
        assertThat(command.description()).isEqualTo("great course");
        assertThat(command.score()).isEqualTo(8);
        assertThat(command.courseId()).isEqualTo(COURSE_ID);
    }

    @Test
    @DisplayName("refuses a submission from a non-student before reaching the use case")
    void submitRejectsNonStudents() {
        when(currentUser.requireStudent())
                .thenThrow(new ForbiddenOperationException("Only students can perform this action"));

        assertThatThrownBy(() -> resource.submit(new FeedbackRequest("great course", 8, COURSE_ID)))
                .isInstanceOf(ForbiddenOperationException.class);

        verifyNoInteractions(submitFeedback);
    }

    @Test
    @DisplayName("returns a paginated envelope to admins")
    void listReturnsAPageForAdmins() {
        when(currentUser.role()).thenReturn(Role.ADMIN);
        when(listAllFeedback.listAll(1, 5)).thenReturn(new FeedbackPage(List.of(FEEDBACK), 1, 5, 11));

        Response response = resource.list(1, 5);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(PagedFeedbackResponse.class);

        PagedFeedbackResponse body = (PagedFeedbackResponse) response.getEntity();
        assertThat(body.items()).hasSize(1);
        assertThat(body.page()).isEqualTo(1);
        assertThat(body.size()).isEqualTo(5);
        assertThat(body.totalElements()).isEqualTo(11);
        assertThat(body.totalPages()).isEqualTo(3);

        verifyNoInteractions(listMyFeedback);
    }

    @Test
    @DisplayName("returns a plain list of their own feedback to students, ignoring the paging params")
    void listReturnsAPlainListForStudents() {
        when(currentUser.role()).thenReturn(Role.STUDENT);
        when(currentUser.localId()).thenReturn(STUDENT_ID);
        when(listMyFeedback.listForStudent(STUDENT_ID)).thenReturn(List.of(FEEDBACK));

        Response response = resource.list(3, 50);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<FeedbackResponse> body = (List<FeedbackResponse>) response.getEntity();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).id()).isEqualTo(7L);

        verifyNoInteractions(listAllFeedback);
    }

    @Test
    @DisplayName("passes the caller-supplied page and size straight through for admins")
    void listForwardsPagingParameters() {
        when(currentUser.role()).thenReturn(Role.ADMIN);
        when(listAllFeedback.listAll(2, 20)).thenReturn(new FeedbackPage(List.of(), 2, 20, 0));

        resource.list(2, 20);

        verify(listAllFeedback).listAll(2, 20);
    }
}
