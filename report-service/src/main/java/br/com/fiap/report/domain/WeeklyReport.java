package br.com.fiap.report.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
