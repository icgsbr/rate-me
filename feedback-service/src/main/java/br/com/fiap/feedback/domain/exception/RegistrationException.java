package br.com.fiap.feedback.domain.exception;

/**
 * Raised when user registration cannot be completed — for instance when the
 * auth-service rejects the credentials or is unreachable.
 */
public class RegistrationException extends RuntimeException {
    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
