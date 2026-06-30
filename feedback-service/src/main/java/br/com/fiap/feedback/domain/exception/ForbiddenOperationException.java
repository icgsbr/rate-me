package br.com.fiap.feedback.domain.exception;

/**
 * Raised when an authenticated caller tries to perform an operation that their
 * (locally resolved) role does not allow — e.g. an admin submitting feedback or a
 * student listing every feedback.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
