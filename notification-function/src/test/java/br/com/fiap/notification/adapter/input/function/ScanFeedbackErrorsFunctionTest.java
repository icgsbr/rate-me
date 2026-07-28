package br.com.fiap.notification.adapter.input.function;

import br.com.fiap.notification.application.port.input.ScanServiceErrorsUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanFeedbackErrorsFunctionTest {

    private static final int LAG_SECONDS = 120;
    private static final int MAX_LOOKBACK_MINUTES = 60;
    private static final int FIRST_RUN_LOOKBACK_MINUTES = 10;

    @Mock
    private ScanServiceErrorsUseCase scanServiceErrors;

    @Captor
    private ArgumentCaptor<OffsetDateTime> fromCaptor;

    @Captor
    private ArgumentCaptor<OffsetDateTime> toCaptor;

    private ScanFeedbackErrorsFunction function;

    @BeforeEach
    void setUp() {
        function = new ScanFeedbackErrorsFunction();
        function.scanServiceErrors = scanServiceErrors;
        function.objectMapper = new ObjectMapper();
        function.settings = settings(true, FIRST_RUN_LOOKBACK_MINUTES);
    }

    @Test
    @DisplayName("skips the scan entirely when the feature is switched off")
    void skipsWhenDisabled() {
        function.settings = settings(false, FIRST_RUN_LOOKBACK_MINUTES);

        function.run(timerInfo("2026-07-28T10:00:00Z"), null);

        verifyNoInteractions(scanServiceErrors);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("falls back to the first-run lookback when the timer carries no payload")
    void usesFirstRunLookbackWithoutTimerInfo(String payload) {
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        function.run(payload, null);

        captureWindow();
        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue()))
                .isEqualTo(Duration.ofMinutes(FIRST_RUN_LOOKBACK_MINUTES));
        assertThat(toCaptor.getValue())
                .isCloseTo(before.minusSeconds(LAG_SECONDS), within(30, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("resumes from the previous run, rewound by the ingestion lag")
    void resumesFromPreviousRun() {
        OffsetDateTime last = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5);

        function.run(timerInfo(last.toString()), null);

        captureWindow();
        assertThat(fromCaptor.getValue())
                .isCloseTo(last.minusSeconds(LAG_SECONDS), within(5, ChronoUnit.SECONDS));
        // to = now - lag, from = last - lag, so the window spans exactly the elapsed time.
        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue()))
                .isCloseTo(Duration.ofMinutes(5), Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("parses the offset-less timestamp format the Functions host emits")
    void parsesLocalDateTimeFormat() {
        LocalDateTime last = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5);

        function.run(timerInfo(last.toString()), null);

        captureWindow();
        assertThat(fromCaptor.getValue())
                .isCloseTo(last.atOffset(ZoneOffset.UTC).minusSeconds(LAG_SECONDS),
                        within(5, ChronoUnit.SECONDS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",                                   // no ScheduleStatus at all
            "{\"ScheduleStatus\":{}}",              // no Last
            "{\"ScheduleStatus\":{\"Last\":\"\"}}", // blank Last
            "{\"ScheduleStatus\":{\"Last\":\"not-a-date\"}}",
            "not even json"
    })
    @DisplayName("treats an unusable timer payload as a first run instead of failing")
    void treatsUnusablePayloadAsFirstRun(String payload) {
        function.run(payload, null);

        captureWindow();
        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue()))
                .isEqualTo(Duration.ofMinutes(FIRST_RUN_LOOKBACK_MINUTES));
    }

    @Test
    @DisplayName("treats the host's never-ran sentinel as a first run")
    void treatsNeverRanSentinelAsFirstRun() {
        function.run(timerInfo("0001-01-01T00:00:00Z"), null);

        captureWindow();
        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue()))
                .isEqualTo(Duration.ofMinutes(FIRST_RUN_LOOKBACK_MINUTES));
    }

    @Test
    @DisplayName("clamps a long outage gap to the maximum lookback")
    void clampsAnOversizedWindow() {
        OffsetDateTime last = OffsetDateTime.now(ZoneOffset.UTC).minusHours(10);

        function.run(timerInfo(last.toString()), null);

        captureWindow();
        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue()))
                .isEqualTo(Duration.ofMinutes(MAX_LOOKBACK_MINUTES));
    }

    @Test
    @DisplayName("skips the scan when the computed window is empty")
    void skipsAnEmptyWindow() {
        function.settings = settings(true, 0);

        function.run(null, null);

        verifyNoInteractions(scanServiceErrors);
    }

    @Test
    @DisplayName("rethrows a scan failure so the Functions host records the invocation as failed")
    void rethrowsScanFailure() {
        when(scanServiceErrors.scan(any(), any())).thenThrow(new IllegalStateException("app id missing"));

        assertThatThrownBy(() -> function.run(null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app id missing");
    }

    private void captureWindow() {
        verify(scanServiceErrors).scan(fromCaptor.capture(), toCaptor.capture());
    }

    private static LogScanSettings settings(boolean enabled, int firstRunLookbackMinutes) {
        return new LogScanSettings(enabled, LAG_SECONDS, MAX_LOOKBACK_MINUTES, firstRunLookbackMinutes);
    }

    private static String timerInfo(String last) {
        return "{\"ScheduleStatus\":{\"Last\":\"%s\"}}".formatted(last);
    }
}
