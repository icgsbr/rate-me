package br.com.fiap.feedback.adapter.input.web.dto;

import br.com.fiap.feedback.application.port.input.ListAllFeedbackUseCase.FeedbackPage;
import br.com.fiap.feedback.application.port.input.RegisterUserUseCase.RegisterUserResult;
import br.com.fiap.feedback.domain.Course;
import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID AUTH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final OffsetDateTime REVIEWED_AT =
            OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    @DisplayName("FeedbackResponse copies every domain field, including the notification state")
    void mapsAFeedback() {
        FeedbackResponse response = FeedbackResponse.from(feedback(7L, true));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        assertThat(response.courseId()).isEqualTo(COURSE_ID);
        assertThat(response.score()).isEqualTo(1);
        assertThat(response.description()).isEqualTo("too fast");
        assertThat(response.reviewDate()).isEqualTo(REVIEWED_AT);
        assertThat(response.notified()).isTrue();
        assertThat(response.notifiedDate()).isEqualTo(REVIEWED_AT.plusMinutes(1));
    }

    @Test
    @DisplayName("FeedbackResponse keeps a not-yet-persisted feedback's null id")
    void mapsAFeedbackWithoutAnId() {
        FeedbackResponse response = FeedbackResponse.from(feedback(null, false));

        assertThat(response.id()).isNull();
        assertThat(response.notified()).isFalse();
        assertThat(response.notifiedDate()).isNull();
    }

    @Test
    @DisplayName("CourseResponse copies the course identity and text")
    void mapsACourse() {
        CourseResponse response = CourseResponse.from(Course.builder()
                .id(COURSE_ID).name("Arquitetura").description("Padroes").build());

        assertThat(response.id()).isEqualTo(COURSE_ID);
        assertThat(response.name()).isEqualTo("Arquitetura");
        assertThat(response.description()).isEqualTo("Padroes");
    }

    @Test
    @DisplayName("RegisterResponse renders the role as its enum name")
    void mapsARegistrationResult() {
        RegisterResponse response = RegisterResponse.from(
                new RegisterUserResult(STUDENT_ID, AUTH_ID, Role.ADMIN));

        assertThat(response.localId()).isEqualTo(STUDENT_ID);
        assertThat(response.authId()).isEqualTo(AUTH_ID);
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("PagedFeedbackResponse maps the items and carries the computed page count")
    void mapsAFeedbackPage() {
        PagedFeedbackResponse response = PagedFeedbackResponse.from(
                new FeedbackPage(List.of(feedback(7L, false), feedback(8L, false)), 1, 5, 11));

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).id()).isEqualTo(7L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("PagedFeedbackResponse handles an empty page")
    void mapsAnEmptyFeedbackPage() {
        PagedFeedbackResponse response =
                PagedFeedbackResponse.from(new FeedbackPage(List.of(), 0, 20, 0));

        assertThat(response.items()).isEmpty();
        assertThat(response.totalPages()).isZero();
    }

    private static Feedback feedback(Long id, boolean notified) {
        return Feedback.builder()
                .id(id)
                .studentId(STUDENT_ID)
                .courseId(COURSE_ID)
                .score(1)
                .reviewDescription("too fast")
                .reviewDate(REVIEWED_AT)
                .notified(notified)
                .notifiedDate(notified ? REVIEWED_AT.plusMinutes(1) : null)
                .build();
    }
}
