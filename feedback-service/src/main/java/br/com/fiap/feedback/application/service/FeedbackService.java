package br.com.fiap.feedback.application.service;

import br.com.fiap.feedback.application.port.input.ListAllFeedbackUseCase;
import br.com.fiap.feedback.application.port.input.ListMyFeedbackUseCase;
import br.com.fiap.feedback.application.port.input.SubmitFeedbackUseCase;
import br.com.fiap.feedback.application.port.output.CourseRepositoryPort;
import br.com.fiap.feedback.application.port.output.FeedbackRepositoryPort;
import br.com.fiap.feedback.application.port.output.NotificationPort;
import br.com.fiap.feedback.application.port.output.StudentRepositoryPort;
import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Student;
import br.com.fiap.feedback.domain.exception.CourseNotFoundException;
import br.com.fiap.feedback.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FeedbackService implements
        SubmitFeedbackUseCase,
        ListMyFeedbackUseCase,
        ListAllFeedbackUseCase {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepositoryPort feedbackRepository;
    private final StudentRepositoryPort studentRepository;
    private final NotificationPort notification;
    private final CourseRepositoryPort courseRepository;

    public FeedbackService(FeedbackRepositoryPort feedbackRepository,
                           StudentRepositoryPort studentRepository,
                           NotificationPort notification,
                           CourseRepositoryPort courseRepository) {
        this.feedbackRepository = feedbackRepository;
        this.studentRepository = studentRepository;
        this.notification = notification;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public Feedback submit(SubmitFeedbackCommand command) {
        int score = command.score();
        if (score < Feedback.MIN_SCORE || score > Feedback.MAX_SCORE) {
            throw new IllegalArgumentException(
                    "score must be between " + Feedback.MIN_SCORE + " and " + Feedback.MAX_SCORE);
        }

        if (!courseRepository.existsById(command.courseId())) {
            throw new CourseNotFoundException("Course not found: " + command.courseId());
        }

        Feedback feedback = Feedback.builder()
                .studentId(command.studentId())
                .courseId(command.courseId())
                .score(score)
                .reviewDescription(command.description())
                .reviewDate(OffsetDateTime.now())
                .notified(false)
                .build();

        feedback = feedbackRepository.save(feedback);
        log.info("Feedback {} submitted by student {} for course {} with score {}",
                feedback.getId(), feedback.getStudentId(), feedback.getCourseId(),
                feedback.getScore());

        if (feedback.isCritical()) {
            feedback = notifyAdmin(feedback);
        }

        return feedback;
    }

    private Feedback notifyAdmin(Feedback feedback) {
        Student student = studentRepository.findByLocalId(feedback.getStudentId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Student not found: " + feedback.getStudentId()));

        log.warn("Critical feedback {} (score {}) from student {} - notifying admin",
                feedback.getId(), feedback.getScore(), student.getId());

        notification.notifyLowScore(feedback, student);

        Feedback notified = feedback.markNotified(OffsetDateTime.now());
        return feedbackRepository.save(notified);
    }

    @Override
    public List<Feedback> listForStudent(UUID studentId) {
        return feedbackRepository.findByStudentId(studentId);
    }

    @Override
    public FeedbackPage listAll(int page, int size) {
        List<Feedback> items = feedbackRepository.findAll(page, size);
        long total = feedbackRepository.countAll();
        return new FeedbackPage(items, page, size, total);
    }
}
