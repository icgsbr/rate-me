package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Feedback;

import java.util.List;

public interface ListAllFeedbackUseCase {

    FeedbackPage listAll(int page, int size);

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
