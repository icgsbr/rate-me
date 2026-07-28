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

public class ScanFeedbackErrorsFunction {

    private static final Logger log = LoggerFactory.getLogger(ScanFeedbackErrorsFunction.class);

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
            log.error("Log scan failed for window {} .. {}", from, to, e);
            throw e;
        }
    }

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
            log.warn("Could not read ScheduleStatus.Last from the timer payload: {}", e.getMessage());
            return null;
        }
    }

    private static OffsetDateTime parseTimestamp(String value) {
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
        }
    }
}
