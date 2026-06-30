package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Feedback;

import java.util.List;

/** Use case for an admin listing every feedback, paginated. */
public interface ListAllFeedbackUseCase {

    FeedbackPage listAll(int page, int size);

    /** A page of feedback plus the pagination metadata needed by the caller. */
    record FeedbackPage(
            List<Feedback> items,
            int page,
            int size,
            long totalElements
    ) {
        public long totalPages() {
            return size <= 0 ? 0 : (totalElements + size - 1) / size;
        }
    }
}
