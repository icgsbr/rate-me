package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.AdminEntity;
import br.com.fiap.feedback.adapter.output.persistence.entity.CourseEntity;
import br.com.fiap.feedback.adapter.output.persistence.entity.FeedbackEntity;
import br.com.fiap.feedback.adapter.output.persistence.entity.StudentEntity;
import br.com.fiap.feedback.domain.Admin;
import br.com.fiap.feedback.domain.Course;
import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lives in the production package because {@link PersistenceMapper} and its methods are
 * deliberately package-private.
 */
class PersistenceMapperTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final OffsetDateTime REVIEWED_AT =
            OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    @DisplayName("a student survives a round trip through the entity unchanged")
    void mapsAStudentBothWays() {
        Student domain = Student.builder()
                .id(ID).name("Alice").registrationNumber("RM12345").authId(AUTH_ID).build();

        StudentEntity entity = PersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(ID);
        assertThat(entity.getName()).isEqualTo("Alice");
        assertThat(entity.getRegistrationNumber()).isEqualTo("RM12345");
        assertThat(entity.getAuthId()).isEqualTo(AUTH_ID);

        Student back = PersistenceMapper.toDomain(entity);
        assertThat(back.getId()).isEqualTo(ID);
        assertThat(back.getName()).isEqualTo("Alice");
        assertThat(back.getRegistrationNumber()).isEqualTo("RM12345");
        assertThat(back.getAuthId()).isEqualTo(AUTH_ID);
    }

    @Test
    @DisplayName("an admin survives a round trip through the entity unchanged")
    void mapsAnAdminBothWays() {
        Admin domain = Admin.builder().id(ID).name("Root").authId(AUTH_ID).role("ADMIN").build();

        AdminEntity entity = PersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(ID);
        assertThat(entity.getName()).isEqualTo("Root");
        assertThat(entity.getAuthId()).isEqualTo(AUTH_ID);
        assertThat(entity.getRole()).isEqualTo("ADMIN");

        Admin back = PersistenceMapper.toDomain(entity);
        assertThat(back.getId()).isEqualTo(ID);
        assertThat(back.getName()).isEqualTo("Root");
        assertThat(back.getAuthId()).isEqualTo(AUTH_ID);
        assertThat(back.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("a course survives a round trip through the entity unchanged")
    void mapsACourseBothWays() {
        Course domain = Course.builder()
                .id(COURSE_ID).name("Arquitetura").description("Padroes").build();

        CourseEntity entity = PersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(COURSE_ID);
        assertThat(entity.getName()).isEqualTo("Arquitetura");
        assertThat(entity.getDescription()).isEqualTo("Padroes");

        Course back = PersistenceMapper.toDomain(entity);
        assertThat(back.getId()).isEqualTo(COURSE_ID);
        assertThat(back.getName()).isEqualTo("Arquitetura");
        assertThat(back.getDescription()).isEqualTo("Padroes");
    }

    @Test
    @DisplayName("a notified feedback survives a round trip with all eight fields")
    void mapsAFeedbackBothWays() {
        Feedback domain = Feedback.builder()
                .id(7L)
                .studentId(ID)
                .courseId(COURSE_ID)
                .score(1)
                .reviewDescription("too fast")
                .reviewDate(REVIEWED_AT)
                .notified(true)
                .notifiedDate(REVIEWED_AT.plusMinutes(1))
                .build();

        FeedbackEntity entity = PersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getStudentId()).isEqualTo(ID);
        assertThat(entity.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(entity.getScore()).isEqualTo(1);
        assertThat(entity.getReviewDescription()).isEqualTo("too fast");
        assertThat(entity.getReviewDate()).isEqualTo(REVIEWED_AT);
        assertThat(entity.isNotified()).isTrue();
        assertThat(entity.getNotifiedDate()).isEqualTo(REVIEWED_AT.plusMinutes(1));

        Feedback back = PersistenceMapper.toDomain(entity);
        assertThat(back.getId()).isEqualTo(7L);
        assertThat(back.getStudentId()).isEqualTo(ID);
        assertThat(back.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(back.getScore()).isEqualTo(1);
        assertThat(back.getReviewDescription()).isEqualTo("too fast");
        assertThat(back.getReviewDate()).isEqualTo(REVIEWED_AT);
        assertThat(back.isNotified()).isTrue();
        assertThat(back.getNotifiedDate()).isEqualTo(REVIEWED_AT.plusMinutes(1));
    }

    @Test
    @DisplayName("a brand-new feedback keeps its null id so the database can assign one")
    void keepsANullFeedbackId() {
        FeedbackEntity entity = PersistenceMapper.toEntity(Feedback.builder()
                .studentId(ID).courseId(COURSE_ID).score(8)
                .reviewDescription("great").reviewDate(REVIEWED_AT).notified(false).build());

        assertThat(entity.getId()).isNull();
        assertThat(entity.getNotifiedDate()).isNull();
        assertThat(entity.isNotified()).isFalse();
    }
}
