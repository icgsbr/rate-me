package br.com.fiap.feedback.application.port.output;

import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Student;

public interface NotificationPort {

    void notifyLowScore(Feedback feedback, Student student);
}
