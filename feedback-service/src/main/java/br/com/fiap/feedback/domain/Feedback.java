package br.com.fiap.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Feedback {

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 10;

    public static final int LOW_SCORE_THRESHOLD = 1;

    private final Long id;

    private final UUID studentId;
    private final UUID courseId;
    private final int score;
    private final String reviewDescription;
    private final OffsetDateTime reviewDate;

    private final boolean notified;
    private final OffsetDateTime notifiedDate;

    public boolean isCritical() {
        return score <= LOW_SCORE_THRESHOLD;
    }

    public Feedback markNotified(OffsetDateTime when) {
        return toBuilder()
                .notified(true)
                .notifiedDate(when)
                .build();
    }
}
