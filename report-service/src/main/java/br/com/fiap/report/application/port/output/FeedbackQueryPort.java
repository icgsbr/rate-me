package br.com.fiap.report.application.port.output;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Read-only output port over the feedback store, used to build reports. */
public interface FeedbackQueryPort {

    /** Feedback whose review date falls within {@code [from, to]}. */
    List<FeedbackRow> findBetween(OffsetDateTime from, OffsetDateTime to);

    /** Minimal projection of a feedback row needed for reporting. */
    record FeedbackRow(
            UUID studentId,
            int score,
            OffsetDateTime reviewDate
    ) {}
}
