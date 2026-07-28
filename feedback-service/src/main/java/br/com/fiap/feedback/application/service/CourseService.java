package br.com.fiap.feedback.application.service;

import br.com.fiap.feedback.application.port.input.CreateCourseUseCase;
import br.com.fiap.feedback.application.port.input.ListCoursesUseCase;
import br.com.fiap.feedback.application.port.output.CourseRepositoryPort;
import br.com.fiap.feedback.domain.Course;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CourseService implements CreateCourseUseCase, ListCoursesUseCase {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepositoryPort courseRepository;

    public CourseService(CourseRepositoryPort courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public Course create(CreateCourseCommand command) {
        Course course = courseRepository.save(Course.builder()
                .id(UUID.randomUUID())
                .name(command.name())
                .description(command.description())
                .build());

        log.info("Course {} registered with name '{}'", course.getId(), course.getName());
        return course;
    }

    @Override
    public List<Course> listAll() {
        return courseRepository.findAllCourses();
    }
}
