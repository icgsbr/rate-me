package br.com.fiap.notification;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scaffold for the critical-event notification.
 *
 * <p>In the cloud this becomes a single-responsibility <strong>Azure Function</strong>
 * triggered whenever an {@code error}-level log is detected by <strong>Azure Monitor +
 * Application Insights</strong>. It would notify the administrator (e.g. via Azure
 * Communication Services) about the failure.</p>
 *
 * <p>No real cloud wiring exists yet; {@link #handle(String)} documents the intended
 * behaviour and keeps the module compiling.</p>
 */
@ApplicationScoped
public class CriticalEventFunction {

    private static final Logger log = LoggerFactory.getLogger(CriticalEventFunction.class);

    /**
     * Handles a critical event payload (the error log/alert from Azure Monitor).
     *
     * @param errorPayload the alert payload that triggered the function
     */
    public void handle(String errorPayload) {
        // TODO(cloud): send an alert to the administrator (Azure Communication Services).
        log.warn("[scaffold] critical event received: {}", errorPayload);
    }
}
