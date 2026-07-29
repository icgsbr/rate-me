package br.com.fiap.report.application.service;

import br.com.fiap.report.application.port.output.FeedbackQueryPort;
import br.com.fiap.report.application.port.output.FeedbackQueryPort.FeedbackRow;
import br.com.fiap.report.application.port.output.ReportNotificationPort;
import br.com.fiap.report.application.port.output.ReportStoragePort;
import br.com.fiap.report.domain.WeeklyReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 7, 21, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = OffsetDateTime.of(2026, 7, 28, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private FeedbackQueryPort feedbackQuery;

    @Mock
    private ReportNotificationPort notification;

    @Mock
    private ReportStoragePort storage;

    @Captor
    private ArgumentCaptor<OffsetDateTime> fromCaptor;

    @Captor
    private ArgumentCaptor<OffsetDateTime> toCaptor;

    private WeeklyReportService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyReportService(feedbackQuery, notification, storage);
    }

    @Test
    @DisplayName("groups evaluations per UTC day in chronological order")
    void groupsEvaluationsPerDay() {
        when(feedbackQuery.findBetween(FROM, TO)).thenReturn(List.of(
                row(ALICE, 8, "2026-07-23T10:00:00Z"),
                row(BOB, 7, "2026-07-21T09:00:00Z"),
                row(ALICE, 9, "2026-07-23T18:00:00Z")));

        WeeklyReport report = service.generate(FROM, TO);

        assertThat(report.evaluationsPerDay())
                .containsExactly(
                        java.util.Map.entry(LocalDate.of(2026, 7, 21), 1L),
                        java.util.Map.entry(LocalDate.of(2026, 7, 23), 2L));
    }

    @Test
    @DisplayName("buckets an evaluation by its UTC day even when the offset says otherwise")
    void normalisesOffsetsToUtcBeforeGrouping() {
        // 2026-07-22T01:00+03:00 is 2026-07-21T22:00Z, so it belongs to the 21st.
        when(feedbackQuery.findBetween(FROM, TO)).thenReturn(List.of(
                new FeedbackRow(ALICE, 8, OffsetDateTime.parse("2026-07-22T01:00:00+03:00"))));

        WeeklyReport report = service.generate(FROM, TO);

        assertThat(report.evaluationsPerDay()).containsOnlyKeys(LocalDate.of(2026, 7, 21));
    }

    @Test
    @DisplayName("counts low scores and lists each affected student only once")
    void countsLowScoresAndDeduplicatesStudents() {
        when(feedbackQuery.findBetween(FROM, TO)).thenReturn(List.of(
                row(ALICE, 0, "2026-07-22T10:00:00Z"),
                row(ALICE, 1, "2026-07-23T10:00:00Z"),
                row(BOB, 2, "2026-07-24T10:00:00Z"),
                row(BOB, 10, "2026-07-25T10:00:00Z")));

        WeeklyReport report = service.generate(FROM, TO);

        // The threshold is inclusive at 1, so score 2 is NOT a low score.
        assertThat(report.lowScoreCount()).isEqualTo(2);
        assertThat(report.lowScoreStudentIds()).containsExactly(ALICE);
        assertThat(report.totalEvaluations()).isEqualTo(4);
        assertThat(report.averageScore()).isEqualTo(3.25);
    }

    @Test
    @DisplayName("reports zeroes instead of NaN when the period has no evaluations")
    void handlesAnEmptyPeriod() {
        when(feedbackQuery.findBetween(FROM, TO)).thenReturn(List.of());

        WeeklyReport report = service.generate(FROM, TO);

        assertThat(report.totalEvaluations()).isZero();
        assertThat(report.averageScore()).isZero();
        assertThat(report.averageEvaluationsPerWeek()).isZero();
        assertThat(report.evaluationsPerDay()).isEmpty();
        assertThat(report.lowScoreStudentIds()).isEmpty();
    }

    @Test
    @DisplayName("averages over the real number of weeks for a longer period")
    void averagesOverMultipleWeeks() {
        OffsetDateTime fourWeeksAgo = TO.minusDays(28);
        when(feedbackQuery.findBetween(fourWeeksAgo, TO)).thenReturn(List.of(
                row(ALICE, 5, "2026-07-01T10:00:00Z"),
                row(ALICE, 5, "2026-07-08T10:00:00Z"),
                row(BOB, 5, "2026-07-15T10:00:00Z"),
                row(BOB, 5, "2026-07-22T10:00:00Z")));

        WeeklyReport report = service.generate(fourWeeksAgo, TO);

        assertThat(report.averageEvaluationsPerWeek()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("clamps the divisor to one week for a zero-length period")
    void clampsAZeroLengthPeriodToOneWeek() {
        when(feedbackQuery.findBetween(TO, TO)).thenReturn(List.of(row(ALICE, 5, "2026-07-28T10:00:00Z")));

        WeeklyReport report = service.generate(TO, TO);

        assertThat(report.averageEvaluationsPerWeek()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("clamps the divisor to one week for an inverted period")
    void clampsAnInvertedPeriodToOneWeek() {
        when(feedbackQuery.findBetween(TO, FROM)).thenReturn(List.of(
                row(ALICE, 5, "2026-07-22T10:00:00Z"),
                row(BOB, 5, "2026-07-23T10:00:00Z")));

        WeeklyReport report = service.generate(TO, FROM);

        assertThat(report.averageEvaluationsPerWeek()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("stores the report before e-mailing it")
    void storesBeforeNotifying() {
        when(feedbackQuery.findBetween(FROM, TO)).thenReturn(List.of(row(ALICE, 5, "2026-07-22T10:00:00Z")));

        WeeklyReport report = service.generate(FROM, TO);

        InOrder order = inOrder(storage, notification);
        order.verify(storage).store(report);
        order.verify(notification).sendWeeklyReport(report);
    }

    @Test
    @DisplayName("does not e-mail the report when storing it fails")
    void doesNotNotifyWhenStorageFails() {
        when(feedbackQuery.findBetween(FROM, TO)).thenReturn(List.of(row(ALICE, 5, "2026-07-22T10:00:00Z")));
        doThrow(new IllegalStateException("storage down")).when(storage).store(any(WeeklyReport.class));

        assertThatThrownBy(() -> service.generate(FROM, TO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("storage down");

        verifyNoInteractions(notification);
    }

    @Test
    @DisplayName("generateLastWeek covers the trailing seven days and publishes the result")
    void generateLastWeekUsesASevenDayWindow() {
        when(feedbackQuery.findBetween(any(), any())).thenReturn(List.of());
        OffsetDateTime before = OffsetDateTime.now();

        service.generateLastWeek();

        verify(feedbackQuery).findBetween(fromCaptor.capture(), toCaptor.capture());
        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue()))
                .isEqualTo(Duration.ofDays(7));
        assertThat(toCaptor.getValue()).isCloseTo(before, within(30, ChronoUnit.SECONDS));

        verify(storage).store(any(WeeklyReport.class));
        verify(notification).sendWeeklyReport(any(WeeklyReport.class));
    }

    @Test
    @DisplayName("previewLastWeek computes the same window but stores and sends nothing")
    void previewHasNoSideEffects() {
        when(feedbackQuery.findBetween(any(), any())).thenReturn(List.of(
                row(ALICE, 3, "2026-07-22T10:00:00Z")));

        WeeklyReport report = service.previewLastWeek();

        verify(feedbackQuery).findBetween(fromCaptor.capture(), toCaptor.capture());
        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue()))
                .isEqualTo(Duration.ofDays(7));
        assertThat(report.totalEvaluations()).isEqualTo(1);

        verifyNoInteractions(storage, notification);
    }

    private static FeedbackRow row(UUID studentId, int score, String reviewDate) {
        return new FeedbackRow(studentId, score, OffsetDateTime.parse(reviewDate));
    }
}
