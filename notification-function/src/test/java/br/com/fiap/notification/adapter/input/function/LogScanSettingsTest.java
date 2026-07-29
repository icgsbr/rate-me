package br.com.fiap.notification.adapter.input.function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogScanSettingsTest {

    @Test
    @DisplayName("exposes each configured value unchanged")
    void exposesConfiguredValues() {
        LogScanSettings settings = new LogScanSettings(true, 120, 60, 10);

        assertThat(settings.enabled()).isTrue();
        assertThat(settings.lagSeconds()).isEqualTo(120);
        assertThat(settings.maxLookbackMinutes()).isEqualTo(60);
        assertThat(settings.firstRunLookbackMinutes()).isEqualTo(10);
    }

    @Test
    @DisplayName("reports the scan as disabled when configured off")
    void reportsDisabled() {
        assertThat(new LogScanSettings(false, 1, 2, 3).enabled()).isFalse();
    }
}
