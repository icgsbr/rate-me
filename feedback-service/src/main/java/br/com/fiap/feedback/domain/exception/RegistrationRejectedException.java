package br.com.fiap.feedback.domain.exception;

/**
 * Raised when the auth-service rejects the registration data itself (e.g. invalid
 * password, duplicate login) - as opposed to an infrastructure failure, see
 * {@link RegistrationException}. Carries the original HTTP status returned by the
 * auth-service so it can be replicated to our own caller.
 */
public class RegistrationRejectedException extends RuntimeException {

    private final int status;

    public RegistrationRejectedException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
