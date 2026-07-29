package br.com.fiap.feedback.domain.exception;

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
