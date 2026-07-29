package br.com.fiap.feedback.application.service;

import br.com.fiap.feedback.application.port.input.CreateCourseUseCase.CreateCourseCommand;
import br.com.fiap.feedback.application.port.output.CourseRepositoryPort;
import br.com.fiap.feedback.domain.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepositoryPort courseRepository;

    @Captor
    private ArgumentCaptor<Course> courseCaptor;

    private CourseService service;

    @BeforeEach
    void setUp() {
        service = new CourseService(courseRepository);
    }

    @Test
    @DisplayName("assigns a locally generated id and persists the submitted name and description")
    void createsACourseWithAGeneratedId() {
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        Course created = service.create(new CreateCourseCommand("Arquitetura", "Padroes e trade-offs"));

        verify(courseRepository).save(courseCaptor.capture());
        Course saved = courseCaptor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Arquitetura");
        assertThat(saved.getDescription()).isEqualTo("Padroes e trade-offs");
        assertThat(created.getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("generates a distinct id for each course")
    void generatesADistinctIdPerCourse() {
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));
        CreateCourseCommand command = new CreateCourseCommand("Arquitetura", "Padroes");

        UUID first = service.create(command).getId();
        UUID second = service.create(command).getId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("lists the registered courses straight from the repository")
    void listsAllCourses() {
        List<Course> courses = List.of(Course.builder().id(UUID.randomUUID()).name("Arquitetura").build());
        when(courseRepository.findAllCourses()).thenReturn(courses);

        assertThat(service.listAll()).isEqualTo(courses);
    }
}
