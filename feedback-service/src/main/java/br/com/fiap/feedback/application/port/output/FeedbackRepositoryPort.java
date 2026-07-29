package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Feedback;

import java.util.List;
import java.util.UUID;

public interface FeedbackRepositoryPort {

    Feedback save(Feedback feedback);

    List<Feedback> findByStudentId(UUID studentId);

    List<Feedback> findAll(int page, int size);

    long countAll();
}
