package br.com.fiap.notification.adapter.input.function;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    public boolean enabled() {
        return enabled;
    }

    public int lagSeconds() {
        return lagSeconds;
    }

    public int maxLookbackMinutes() {
        return maxLookbackMinutes;
    }

    public int firstRunLookbackMinutes() {
        return firstRunLookbackMinutes;
    }
}
