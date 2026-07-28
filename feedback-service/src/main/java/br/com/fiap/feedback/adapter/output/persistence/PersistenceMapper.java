package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.AdminEntity;
import br.com.fiap.feedback.adapter.output.persistence.entity.CourseEntity;
import br.com.fiap.feedback.adapter.output.persistence.entity.FeedbackEntity;
import br.com.fiap.feedback.adapter.output.persistence.entity.StudentEntity;
import br.com.fiap.feedback.domain.Admin;
import br.com.fiap.feedback.domain.Course;
import br.com.fiap.feedback.domain.Feedback;
import br.com.fiap.feedback.domain.Student;

/**
 * Translates between domain models and JPA entities, keeping persistence concerns out
 * of the domain. Stateless static helpers.
 */
final class PersistenceMapper {

    private PersistenceMapper() {
    }

    static Student toDomain(StudentEntity e) {
        return Student.builder()
                .id(e.getId())
                .name(e.getName())
                .registrationNumber(e.getRegistrationNumber())
                .authId(e.getAuthId())
                .build();
    }

    static StudentEntity toEntity(Student s) {
        StudentEntity e = new StudentEntity();
        e.setId(s.getId());
        e.setName(s.getName());
        e.setRegistrationNumber(s.getRegistrationNumber());
        e.setAuthId(s.getAuthId());
        return e;
    }

    static Admin toDomain(AdminEntity e) {
        return Admin.builder()
                .id(e.getId())
                .name(e.getName())
                .authId(e.getAuthId())
                .role(e.getRole())
                .build();
    }

    static AdminEntity toEntity(Admin a) {
        AdminEntity e = new AdminEntity();
        e.setId(a.getId());
        e.setName(a.getName());
        e.setAuthId(a.getAuthId());
        e.setRole(a.getRole());
        return e;
    }

    static Course toDomain(CourseEntity e) {
        return Course.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }

    static CourseEntity toEntity(Course c) {
        CourseEntity e = new CourseEntity();
        e.setId(c.getId());
        e.setName(c.getName());
        e.setDescription(c.getDescription());
        return e;
    }

    static Feedback toDomain(FeedbackEntity e) {
        return Feedback.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .courseId(e.getCourseId())
                .score(e.getScore())
                .reviewDescription(e.getReviewDescription())
                .reviewDate(e.getReviewDate())
                .notified(e.isNotified())
                .notifiedDate(e.getNotifiedDate())
                .build();
    }

    static FeedbackEntity toEntity(Feedback f) {
        FeedbackEntity e = new FeedbackEntity();
        e.setId(f.getId());
        e.setStudentId(f.getStudentId());
        e.setCourseId(f.getCourseId());
        e.setScore(f.getScore());
        e.setReviewDescription(f.getReviewDescription());
        e.setReviewDate(f.getReviewDate());
        e.setNotified(f.isNotified());
        e.setNotifiedDate(f.getNotifiedDate());
        return e;
    }
}
