package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Feedback;

import java.util.UUID;

/**
 * Use case for submitting a new feedback as a student.
 *
 * <p>If the score is in the critical range the implementation also triggers the
 * low-score notification and flags the feedback as notified.</p>
 */
public interface SubmitFeedbackUseCase {

    Feedback submit(SubmitFeedbackCommand command);

    /**
     * @param studentId   local id of the authenticated student
     * @param description  free-text review
     * @param score        rating from {@link Feedback#MIN_SCORE} to {@link Feedback#MAX_SCORE}
     */
    record SubmitFeedbackCommand(
            UUID studentId,
            String description,
            int score
    ) {}
}
