package br.com.fiap.notification.domain;

public class InvalidNotificationException extends RuntimeException {

    public InvalidNotificationException(String message) {
        super(message);
    }
}
