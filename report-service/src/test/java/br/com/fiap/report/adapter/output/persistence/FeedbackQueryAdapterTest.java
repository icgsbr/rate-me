package br.com.fiap.report.adapter.output.persistence;

import br.com.fiap.report.application.port.output.FeedbackQueryPort.FeedbackRow;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Panache query methods are {@code default} methods on the repository interface, which
 * makes them ordinary virtual calls: a Mockito spy can stub them, so the adapter's own
 * mapping logic is exercised without a database.
 */
class FeedbackQueryAdapterTest {

    private static final String QUERY = "reviewDate between ?1 and ?2";

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 7, 21, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = OffsetDateTime.of(2026, 7, 28, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    @DisplayName("maps every matching entity onto a FeedbackRow, preserving order")
    void mapsEntitiesToRows() {
        OffsetDateTime first = OffsetDateTime.of(2026, 7, 22, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime second = OffsetDateTime.of(2026, 7, 24, 15, 30, 0, 0, ZoneOffset.UTC);

        FeedbackQueryAdapter adapter = adapterReturning(entity(1L, ALICE, 8, first),
                entity(2L, BOB, 1, second));

        List<FeedbackRow> rows = adapter.findBetween(FROM, TO);

        assertThat(rows).containsExactly(
                new FeedbackRow(ALICE, 8, first),
                new FeedbackRow(BOB, 1, second));
    }

    @Test
    @DisplayName("returns an empty list when nothing falls inside the period")
    void returnsEmptyListWhenNothingMatches() {
        assertThat(adapterReturning().findBetween(FROM, TO)).isEmpty();
    }

    @Test
    @DisplayName("queries by the review date range with the period bounds as parameters")
    void queriesByReviewDateRange() {
        FeedbackQueryAdapter adapter = adapterReturning();

        adapter.findBetween(FROM, TO);

        verify(adapter).find(QUERY, FROM, TO);
    }

    @SuppressWarnings("unchecked")
    private static FeedbackQueryAdapter adapterReturning(FeedbackReadEntity... entities) {
        FeedbackQueryAdapter adapter = spy(new FeedbackQueryAdapter());
        PanacheQuery<FeedbackReadEntity> query = mock(PanacheQuery.class);

        when(query.list()).thenReturn(List.of(entities));
        doReturn(query).when(adapter).find(QUERY, FROM, TO);

        return adapter;
    }

    /** The read entity is intentionally setter-free, so populate it reflectively. */
    private static FeedbackReadEntity entity(Long id, UUID studentId, int score, OffsetDateTime reviewDate) {
        FeedbackReadEntity entity = new FeedbackReadEntity();
        set(entity, "id", id);
        set(entity, "studentId", studentId);
        set(entity, "score", score);
        set(entity, "reviewDate", reviewDate);
        return entity;
    }

    private static void set(FeedbackReadEntity entity, String fieldName, Object value) {
        try {
            Field field = FeedbackReadEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not populate " + fieldName + " for the test", e);
        }
    }
}
