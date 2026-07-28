package br.com.fiap.feedback.domain.exception;

/**
 * Raised when a feedback submission references a course that does not exist.
 */
public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(String message) {
        super(message);
    }
}
