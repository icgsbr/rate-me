package br.com.fiap.report.adapter.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only mapping of the {@code feedback} table owned by feedback-service.
 *
 * <p>report-service never writes to this table; only the columns needed for reporting
 * are mapped.</p>
 */
@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor
public class FeedbackReadEntity {

    @Id
    private Long id;

    @Column(name = "student_id")
    private UUID studentId;

    private int score;

    @Column(name = "review_date")
    private OffsetDateTime reviewDate;
}
