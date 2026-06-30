package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Feedback;

import java.util.List;
import java.util.UUID;

/** Output port for persisting and querying feedback. */
public interface FeedbackRepositoryPort {

    /** Persists a new feedback (or updates an existing one) and returns it with its id. */
    Feedback save(Feedback feedback);

    /** All feedback authored by a given student, newest first. */
    List<Feedback> findByStudentId(UUID studentId);

    /** A page of all feedback, newest first (admin listing). */
    List<Feedback> findAll(int page, int size);

    /** Total number of feedback rows, for pagination metadata. */
    long countAll();
}
