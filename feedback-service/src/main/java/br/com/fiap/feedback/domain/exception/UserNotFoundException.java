package br.com.fiap.feedback.domain.exception;

/**
 * Raised when an authenticated caller has no matching local student/admin record
 * (resolved by the {@code authId} JWT claim).
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
