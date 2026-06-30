package br.com.fiap.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single feedback/rating left by a student for a course.
 *
 * <p>The score ranges from {@value #MIN_SCORE} to {@value #MAX_SCORE}. A score at or
 * below {@value #LOW_SCORE_THRESHOLD} is considered critical and triggers an alert to
 * the administrator (see the notification flow); once the alert is sent the feedback is
 * marked as {@link #notified}.</p>
 */
@Getter
@Builder(toBuilder = true)
public class Feedback {

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 10;

    /** Scores at or below this value are critical and trigger an admin alert. */
    public static final int LOW_SCORE_THRESHOLD = 1;

    /** Database-generated identity; {@code null} before the feedback is persisted. */
    private final Long id;

    private final UUID studentId;
    private final UUID courseId;
    private final int score;
    private final String reviewDescription;
    private final OffsetDateTime reviewDate;

    private final boolean notified;
    private final OffsetDateTime notifiedDate;

    /** Whether this feedback's score is in the critical (low) range. */
    public boolean isCritical() {
        return score <= LOW_SCORE_THRESHOLD;
    }

    /**
     * Returns a copy of this feedback flagged as notified at the given instant.
     * The domain model is immutable, so state transitions return new instances.
     */
    public Feedback markNotified(OffsetDateTime when) {
        return toBuilder()
                .notified(true)
                .notifiedDate(when)
                .build();
    }
}
