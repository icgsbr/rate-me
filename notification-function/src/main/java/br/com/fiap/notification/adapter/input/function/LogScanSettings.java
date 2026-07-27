package br.com.fiap.notification.adapter.input.function;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Timing knobs for the log scan, as a bean so the function class can get them through the
 * same CDI injection it already uses for the use cases.
 */
@ApplicationScoped
public class LogScanSettings {

    private final boolean enabled;
    private final int lagSeconds;
    private final int maxLookbackMinutes;
    private final int firstRunLookbackMinutes;

    public LogScanSettings(
            @ConfigProperty(name = "app.log-scan.enabled") boolean enabled,
            @ConfigProperty(name = "app.log-scan.lag-seconds") int lagSeconds,
            @ConfigProperty(name = "app.log-scan.max-lookback-minutes") int maxLookbackMinutes,
            @ConfigProperty(name = "app.log-scan.first-run-lookback-minutes") int firstRunLookbackMinutes) {
        this.enabled = enabled;
        this.lagSeconds = lagSeconds;
        this.maxLookbackMinutes = maxLookbackMinutes;
        this.firstRunLookbackMinutes = firstRunLookbackMinutes;
    }

    /** Lets the scan be switched off from the portal without a redeploy. */
    public boolean enabled() {
        return enabled;
    }

    /**
     * How far behind "now" the window ends. Application Insights takes a couple of minutes
     * to make a record queryable, so scanning right up to now would miss the newest errors
     * and never come back for them.
     */
    public int lagSeconds() {
        return lagSeconds;
    }

    /** Ceiling on a single window, so a long outage does not replay hours of history. */
    public int maxLookbackMinutes() {
        return maxLookbackMinutes;
    }

    /** Window used when the host has no previous execution recorded yet. */
    public int firstRunLookbackMinutes() {
        return firstRunLookbackMinutes;
    }
}
