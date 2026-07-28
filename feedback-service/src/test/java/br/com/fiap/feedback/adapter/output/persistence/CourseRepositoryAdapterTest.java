package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.CourseEntity;
import br.com.fiap.feedback.domain.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class CourseRepositoryAdapterTest {

    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private CourseRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = spy(new CourseRepositoryAdapter());
    }

    @Test
    @DisplayName("persists a course and maps the entity back to the domain")
    void savesACourse() {
        doNothing().when(adapter).persist(any(CourseEntity.class));

        Course saved = adapter.save(course());

        verify(adapter).persist(any(CourseEntity.class));
        assertThat(saved.getId()).isEqualTo(COURSE_ID);
        assertThat(saved.getName()).isEqualTo("Arquitetura");
        assertThat(saved.getDescription()).isEqualTo("Padroes");
    }

    @Test
    @DisplayName("maps every stored course when listing")
    void listsAllCourses() {
        doReturn(List.of(PersistenceMapper.toEntity(course()))).when(adapter).listAll();

        List<Course> courses = adapter.findAllCourses();

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getId()).isEqualTo(COURSE_ID);
        assertThat(courses.get(0).getName()).isEqualTo("Arquitetura");
    }

    @Test
    @DisplayName("returns an empty list when no course is registered")
    void listsNoCourses() {
        doReturn(List.of()).when(adapter).listAll();

        assertThat(adapter.findAllCourses()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"0, false", "1, true", "2, true"})
    @DisplayName("reports existence from the row count for the given id")
    void checksExistenceByCount(long count, boolean expected) {
        doReturn(count).when(adapter).count("id", COURSE_ID);

        assertThat(adapter.existsById(COURSE_ID)).isEqualTo(expected);
    }

    private static Course course() {
        return Course.builder().id(COURSE_ID).name("Arquitetura").description("Padroes").build();
    }
}
