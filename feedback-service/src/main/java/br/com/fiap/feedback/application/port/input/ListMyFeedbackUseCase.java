package br.com.fiap.feedback.application.port.input;

import br.com.fiap.feedback.domain.Feedback;

import java.util.List;
import java.util.UUID;

/** Use case for a student listing their own feedback. */
public interface ListMyFeedbackUseCase {

    List<Feedback> listForStudent(UUID studentId);
}
