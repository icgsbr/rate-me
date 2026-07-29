package br.com.fiap.feedback.application.service;

import br.com.fiap.feedback.application.port.input.ListAllFeedbackUseCase.FeedbackPage;
import br.com.fiap.feedback.application.port.input.SubmitFeedbackUseCase.SubmitFeedbackCommand;
import br.com.fiap.feedback.application.port.output.CourseRepositoryPort;
import br.com.fiap.feedback.application.port.output.FeedbackRepositoryPort;
import br.com.fiap.feedback.application.port.output.NotificationPort;
import br.com.fiap.feedback.application.port.output.StudentRepositoryPort;
import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Student;
import br.com.fiap.feedback.domain.exception.CourseNotFoundException;
import br.com.fiap.feedback.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final Student STUDENT = Student.builder()
            .id(STUDENT_ID)
            .name("Alice")
            .registrationNumber("RM12345")
            .authId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
            .build();

    @Mock
    private FeedbackRepositoryPort feedbackRepository;

    @Mock
    private StudentRepositoryPort studentRepository;

    @Mock
    private NotificationPort notification;

    @Mock
    private CourseRepositoryPort courseRepository;

    @Captor
    private ArgumentCaptor<Feedback> feedbackCaptor;

    private FeedbackService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackService(feedbackRepository, studentRepository, notification, courseRepository);
    }

    // --- submit: validation ------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {-1, 11, 100})
    @DisplayName("rejects a score outside the allowed range before touching any repository")
    void rejectsAnOutOfRangeScore(int score) {
        assertThatThrownBy(() -> service.submit(command(score)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("score must be between 0 and 10");

        verifyNoInteractions(courseRepository, feedbackRepository, studentRepository, notification);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10})
    @DisplayName("accepts the score range boundaries")
    void acceptsTheScoreBoundaries(int score) {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
        // Only score 0 is critical, so this lookup is unused on the upper boundary.
        lenient().when(studentRepository.findByLocalId(STUDENT_ID)).thenReturn(Optional.of(STUDENT));

        assertThat(service.submit(command(score)).getScore()).isEqualTo(score);
    }

    @Test
    @DisplayName("rejects a submission for a course that does not exist")
    void rejectsAnUnknownCourse() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.submit(command(8)))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessage("Course not found: " + COURSE_ID);

        verifyNoInteractions(feedbackRepository, notification);
    }

    // --- submit: non-critical path -----------------------------------------------------

    @Test
    @DisplayName("saves a non-critical feedback once and alerts nobody")
    void savesANonCriticalFeedbackWithoutAlerting() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
        OffsetDateTime before = OffsetDateTime.now();

        Feedback saved = service.submit(command(8));

        verify(feedbackRepository).save(feedbackCaptor.capture());
        Feedback submitted = feedbackCaptor.getValue();
        assertThat(submitted.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(submitted.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(submitted.getReviewDescription()).isEqualTo("the pace was too fast");
        assertThat(submitted.isNotified()).isFalse();
        assertThat(submitted.getReviewDate()).isBetween(before, OffsetDateTime.now());

        assertThat(saved.isNotified()).isFalse();
        verifyNoInteractions(notification, studentRepository);
    }

    // --- submit: critical path ---------------------------------------------------------

    @Test
    @DisplayName("alerts the admin and persists the notified flag for a critical score")
    void alertsAndFlagsACriticalFeedback() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.findByLocalId(STUDENT_ID)).thenReturn(Optional.of(STUDENT));
        OffsetDateTime before = OffsetDateTime.now();

        Feedback result = service.submit(command(1));

        // Saved twice: once on insert, once to persist the notified flag.
        verify(feedbackRepository, times(2)).save(feedbackCaptor.capture());
        List<Feedback> saves = feedbackCaptor.getAllValues();
        assertThat(saves.get(0).isNotified()).isFalse();
        assertThat(saves.get(1).isNotified()).isTrue();
        assertThat(saves.get(1).getNotifiedDate()).isBetween(before, OffsetDateTime.now());

        verify(notification).notifyLowScore(any(Feedback.class), any(Student.class));
        assertThat(result.isNotified()).isTrue();
    }

    @Test
    @DisplayName("notifies before persisting the flag, so a failed alert is not recorded as sent")
    void notifiesBeforeFlagging() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.findByLocalId(STUDENT_ID)).thenReturn(Optional.of(STUDENT));

        service.submit(command(0));

        InOrder order = inOrder(notification, feedbackRepository);
        order.verify(notification).notifyLowScore(any(Feedback.class), any(Student.class));
        order.verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("fails the critical submission when the author has no local record")
    void failsWhenTheCriticalAuthorIsUnknown() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.findByLocalId(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(command(0)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Student not found: " + STUDENT_ID);

        verifyNoInteractions(notification);
        // Only the initial insert happened; the notified flag was never persisted.
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    // --- listings ----------------------------------------------------------------------

    @Test
    @DisplayName("lists a student's own feedback straight from the repository")
    void listsForStudent() {
        List<Feedback> expected = List.of(Feedback.builder().id(1L).score(5).build());
        when(feedbackRepository.findByStudentId(STUDENT_ID)).thenReturn(expected);

        assertThat(service.listForStudent(STUDENT_ID)).isEqualTo(expected);
    }

    @Test
    @DisplayName("wraps the admin listing together with its pagination metadata")
    void listsAllWithPaginationMetadata() {
        List<Feedback> items = List.of(Feedback.builder().id(1L).score(5).build());
        when(feedbackRepository.findAll(2, 20)).thenReturn(items);
        when(feedbackRepository.countAll()).thenReturn(45L);

        FeedbackPage page = service.listAll(2, 20);

        assertThat(page.items()).isEqualTo(items);
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(45);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    private static SubmitFeedbackCommand command(int score) {
        return new SubmitFeedbackCommand(STUDENT_ID, "the pace was too fast", score, COURSE_ID);
    }
}
