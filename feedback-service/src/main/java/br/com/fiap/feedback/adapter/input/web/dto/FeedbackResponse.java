package br.com.fiap.feedback.adapter.input.web.dto;

import br.com.fiap.feedback.domain.Feedback;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeedbackResponse(
        Long id,
        UUID studentId,
        UUID courseId,
        int score,
        String description,
        OffsetDateTime reviewDate,
        boolean notified,
        OffsetDateTime notifiedDate
) {
    public static FeedbackResponse from(Feedback f) {
        return new FeedbackResponse(
                f.getId(),
                f.getStudentId(),
                f.getCourseId(),
                f.getScore(),
                f.getReviewDescription(),
                f.getReviewDate(),
                f.isNotified(),
                f.getNotifiedDate());
    }
}
