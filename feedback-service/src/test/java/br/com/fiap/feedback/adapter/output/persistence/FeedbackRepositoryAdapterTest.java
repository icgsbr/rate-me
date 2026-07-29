package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.FeedbackEntity;
import br.com.fiap.feedback.domain.Feedback;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Panache query methods are {@code default} methods on the repository interface, so a
 * Mockito spy can stub them and the adapter's own logic runs without a database.
 */
class FeedbackRepositoryAdapterTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime REVIEWED_AT =
            OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime NOTIFIED_AT = REVIEWED_AT.plusMinutes(1);

    private FeedbackRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = spy(new FeedbackRepositoryAdapter());
    }

    @Test
    @DisplayName("inserts a feedback that has no id yet")
    void insertsANewFeedback() {
        doNothing().when(adapter).persist(any(FeedbackEntity.class));

        Feedback saved = adapter.save(feedback(null, false, null));

        verify(adapter).persist(any(FeedbackEntity.class));
        verify(adapter, never()).findById(any());
        assertThat(saved.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(saved.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(saved.getScore()).isEqualTo(1);
        assertThat(saved.isNotified()).isFalse();
    }

    @Test
    @DisplayName("updates only the notification fields of an already persisted feedback")
    void updatesOnlyTheNotificationFields() {
        FeedbackEntity managed = PersistenceMapper.toEntity(feedback(7L, false, null));
        doReturn(managed).when(adapter).findById(7L);

        // Everything but the notification state differs from the managed row.
        Feedback incoming = Feedback.builder()
                .id(7L)
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .score(9)
                .reviewDescription("edited afterwards")
                .reviewDate(REVIEWED_AT.plusDays(1))
                .notified(true)
                .notifiedDate(NOTIFIED_AT)
                .build();

        Feedback saved = adapter.save(incoming);

        verify(adapter, never()).persist(any(FeedbackEntity.class));
        assertThat(saved.isNotified()).isTrue();
        assertThat(saved.getNotifiedDate()).isEqualTo(NOTIFIED_AT);
        // The other edits are deliberately discarded: only the flag is flushed.
        assertThat(saved.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(saved.getScore()).isEqualTo(1);
        assertThat(saved.getReviewDescription()).isEqualTo("too fast");
    }

    @Test
    @DisplayName("counts every feedback row for the pagination metadata")
    void countsAllRows() {
        doReturn(42L).when(adapter).count();

        assertThat(adapter.countAll()).isEqualTo(42L);
    }

    @Test
    @DisplayName("returns a student's feedback newest first")
    void findsByStudentNewestFirst() {
        PanacheQuery<FeedbackEntity> query = panacheQuery(
                PersistenceMapper.toEntity(feedback(7L, false, null)),
                PersistenceMapper.toEntity(feedback(8L, true, NOTIFIED_AT)));
        doReturn(query).when(adapter).find(eq("studentId"), any(Sort.class), eq(STUDENT_ID));

        List<Feedback> found = adapter.findByStudentId(STUDENT_ID);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getId()).isEqualTo(7L);
        assertThat(found.get(1).isNotified()).isTrue();
    }

    @Test
    @DisplayName("returns an empty list when the student has no feedback")
    void findsNothingForAStudentWithoutFeedback() {
        doReturn(panacheQuery()).when(adapter).find(eq("studentId"), any(Sort.class), eq(STUDENT_ID));

        assertThat(adapter.findByStudentId(STUDENT_ID)).isEmpty();
    }

    @Test
    @DisplayName("applies the requested page window to the admin listing")
    void findsAPageOfAllFeedback() {
        PanacheQuery<FeedbackEntity> query =
                panacheQuery(PersistenceMapper.toEntity(feedback(7L, false, null)));
        doReturn(query).when(adapter).findAll(any(Sort.class));

        List<Feedback> found = adapter.findAll(2, 5);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(7L);

        // Page has no equals(), so assert on its fields rather than on the instance.
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(query).page(pageCaptor.capture());
        assertThat(pageCaptor.getValue().index).isEqualTo(2);
        assertThat(pageCaptor.getValue().size).isEqualTo(5);
    }

    @SuppressWarnings("unchecked")
    private static PanacheQuery<FeedbackEntity> panacheQuery(FeedbackEntity... entities) {
        PanacheQuery<FeedbackEntity> query = mock(PanacheQuery.class);
        when(query.list()).thenReturn(List.of(entities));
        org.mockito.Mockito.lenient().when(query.page(any(Page.class))).thenReturn(query);
        return query;
    }

    private static Feedback feedback(Long id, boolean notified, OffsetDateTime notifiedDate) {
        return Feedback.builder()
                .id(id)
                .studentId(STUDENT_ID)
                .courseId(COURSE_ID)
                .score(1)
                .reviewDescription("too fast")
                .reviewDate(REVIEWED_AT)
                .notified(notified)
                .notifiedDate(notifiedDate)
                .build();
    }
}
