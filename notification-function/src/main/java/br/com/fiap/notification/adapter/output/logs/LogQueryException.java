package br.com.fiap.notification.adapter.output.logs;

/** The monitored service's logs could not be read. */
public class LogQueryException extends RuntimeException {

    public LogQueryException(String message) {
        super(message);
    }

    public LogQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
