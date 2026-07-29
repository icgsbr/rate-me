package br.com.fiap.feedback.adapter.input.web.dto;

import br.com.fiap.feedback.application.port.input.ListAllFeedbackUseCase.FeedbackPage;

import java.util.List;

public record PagedFeedbackResponse(
        List<FeedbackResponse> items,
        int page,
        int size,
        long totalElements,
        long totalPages
) {
    public static PagedFeedbackResponse from(FeedbackPage page) {
        List<FeedbackResponse> items = page.items().stream()
                .map(FeedbackResponse::from)
                .toList();
        return new PagedFeedbackResponse(
                items, page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
