package br.com.fiap.feedback.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackTest {

    private static final OffsetDateTime REVIEWED_AT =
            OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime NOTIFIED_AT = REVIEWED_AT.plusMinutes(1);

    @ParameterizedTest
    @CsvSource({
            "0,  true",
            "1,  true",
            "2,  false",
            "10, false"
    })
    @DisplayName("treats scores at or below the threshold as critical")
    void flagsCriticalScores(int score, boolean expected) {
        assertThat(feedback(score).isCritical()).isEqualTo(expected);
    }

    @Test
    @DisplayName("markNotified returns a flagged copy and leaves the original untouched")
    void markNotifiedIsImmutable() {
        Feedback original = feedback(0);

        Feedback notified = original.markNotified(NOTIFIED_AT);

        assertThat(notified).isNotSameAs(original);
        assertThat(notified.isNotified()).isTrue();
        assertThat(notified.getNotifiedDate()).isEqualTo(NOTIFIED_AT);

        assertThat(original.isNotified()).isFalse();
        assertThat(original.getNotifiedDate()).isNull();
    }

    @Test
    @DisplayName("markNotified carries every other field over unchanged")
    void markNotifiedPreservesTheRemainingState() {
        Feedback original = feedback(1);

        Feedback notified = original.markNotified(NOTIFIED_AT);

        assertThat(notified.getId()).isEqualTo(original.getId());
        assertThat(notified.getStudentId()).isEqualTo(original.getStudentId());
        assertThat(notified.getCourseId()).isEqualTo(original.getCourseId());
        assertThat(notified.getScore()).isEqualTo(original.getScore());
        assertThat(notified.getReviewDescription()).isEqualTo(original.getReviewDescription());
        assertThat(notified.getReviewDate()).isEqualTo(original.getReviewDate());
    }

    @Test
    @DisplayName("declares the documented score bounds")
    void declaresScoreBounds() {
        assertThat(Feedback.MIN_SCORE).isZero();
        assertThat(Feedback.MAX_SCORE).isEqualTo(10);
        assertThat(Feedback.LOW_SCORE_THRESHOLD).isEqualTo(1);
    }

    private static Feedback feedback(int score) {
        return Feedback.builder()
                .id(42L)
                .studentId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .courseId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .score(score)
                .reviewDescription("the pace was too fast")
                .reviewDate(REVIEWED_AT)
                .notified(false)
                .build();
    }
}
