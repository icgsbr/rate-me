package br.com.fiap.report.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregated weekly feedback report.
 *
 * <p>Captures every metric required by the challenge:
 * evaluations per day, number of low-score evaluations (and which students authored
 * them), average evaluations per week and the average score over the period.</p>
 */
public record WeeklyReport(
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        long totalEvaluations,
        Map<LocalDate, Long> evaluationsPerDay,
        long lowScoreCount,
        List<UUID> lowScoreStudentIds,
        double averageEvaluationsPerWeek,
        double averageScore
) {}
