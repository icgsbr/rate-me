package br.com.fiap.notification.domain;

/** Raised when an incoming alert payload does not describe a valid critical event. */
public class InvalidNotificationException extends RuntimeException {

    public InvalidNotificationException(String message) {
        super(message);
    }
}
