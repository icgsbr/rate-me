package br.com.fiap.notification.adapter.input.function;

import br.com.fiap.notification.application.port.input.ScanServiceErrorsUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Timer-triggered Azure Function that watches the feedback-service logs and escalates any
 * ERROR-level record to the administrators.
 *
 * <p>This is the "notification service keeps analysing the feedback-service logs" half of
 * the platform; {@link CriticalNotificationFunction} is the push half. Both end up in the
 * same {@code AlertSenderPort}.</p>
 *
 * <p>The schedule comes from the {@code LOG_SCAN_SCHEDULE} app setting (the {@code %...%}
 * syntax the Functions host resolves), because the annotation needs a compile-time
 * constant but the cadence has to be tunable from the portal.</p>
 */
public class ScanFeedbackErrorsFunction {

    private static final Logger log = LoggerFactory.getLogger(ScanFeedbackErrorsFunction.class);

    /** What the host writes into ScheduleStatus.Last before the first real execution. */
    private static final OffsetDateTime NEVER_RAN = OffsetDateTime.of(
            1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Inject
    ScanServiceErrorsUseCase scanServiceErrors;

    @Inject
    LogScanSettings settings;

    @Inject
    ObjectMapper objectMapper;

    @FunctionName("scanFeedbackErrors")
    public void run(
            @TimerTrigger(name = "timer", schedule = "%LOG_SCAN_SCHEDULE%") String timerInfo,
            final ExecutionContext context) {

        if (!settings.enabled()) {
            log.info("Log scan is disabled (app.log-scan.enabled=false); skipping");
            return;
        }

        Duration lag = Duration.ofSeconds(settings.lagSeconds());
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minus(lag);

        // Windows are anchored on the previous execution and shifted back by the same lag,
        // so consecutive runs tile the timeline exactly: no gap, no record scanned twice.
        // The host persists ScheduleStatus in AzureWebJobsStorage, so this survives restarts.
        OffsetDateTime previousRun = previousRun(timerInfo);
        OffsetDateTime from = previousRun == null
                ? to.minusMinutes(settings.firstRunLookbackMinutes())
                : previousRun.minus(lag);

        OffsetDateTime earliest = to.minusMinutes(settings.maxLookbackMinutes());
        if (from.isBefore(earliest)) {
            log.warn("Previous run was {} - clamping the window to the last {} minutes",
                    previousRun, settings.maxLookbackMinutes());
            from = earliest;
        }

        if (!from.isBefore(to)) {
            log.debug("Empty scan window ({} .. {}); skipping", from, to);
            return;
        }

        try {
            int found = scanServiceErrors.scan(from, to);
            log.info("Log scan finished: {} error(s) in window {} .. {}", found, from, to);
        } catch (Exception e) {
            // Let the invocation fail so the failure is visible in Application Insights
            // instead of a scan that silently stops watching.
            log.error("Log scan failed for window {} .. {}", from, to, e);
            throw e;
        }
    }

    /**
     * Reads {@code ScheduleStatus.Last} out of the timer payload the host passes in.
     *
     * @return the previous execution instant, or {@code null} when there is not one yet
     */
    private OffsetDateTime previousRun(String timerInfo) {
        if (timerInfo == null || timerInfo.isBlank()) {
            return null;
        }
        try {
            JsonNode last = objectMapper.readTree(timerInfo).path("ScheduleStatus").path("Last");
            if (last.isMissingNode() || last.asText("").isBlank()) {
                return null;
            }
            OffsetDateTime parsed = parseTimestamp(last.asText());
            return parsed.isAfter(NEVER_RAN) ? parsed : null;
        } catch (Exception e) {
            // A malformed payload must not stop the scan; fall back to the first-run window.
            log.warn("Could not read ScheduleStatus.Last from the timer payload: {}", e.getMessage());
            return null;
        }
    }

    /** The host may or may not include an offset; treat a bare timestamp as UTC. */
    private static OffsetDateTime parseTimestamp(String value) {
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
        }
    }
}
