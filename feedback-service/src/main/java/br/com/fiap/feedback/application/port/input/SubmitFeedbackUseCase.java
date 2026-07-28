package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Feedback;

import java.util.UUID;

public interface SubmitFeedbackUseCase {

    Feedback submit(SubmitFeedbackCommand command);

    record SubmitFeedbackCommand(
            UUID studentId,
            String description,
            int score
    ) {}
}
