package br.com.fiap.report.application.port.output;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FeedbackQueryPort {

    List<FeedbackRow> findBetween(OffsetDateTime from, OffsetDateTime to);

    record FeedbackRow(
            UUID studentId,
            int score,
            OffsetDateTime reviewDate
    ) {}
}
