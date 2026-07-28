package br.com.fiap.feedback.adapter.input.web.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the Bean Validation constraints on the inbound payloads with a real
 * validator, so the annotations are checked rather than assumed.
 */
class RequestValidationTest {

    private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void startValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    // --- FeedbackRequest ---------------------------------------------------------------

    @Test
    @DisplayName("a complete feedback payload passes validation")
    void acceptsAValidFeedbackRequest() {
        assertThat(validator.validate(new FeedbackRequest("great course", 8, COURSE_ID))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("a feedback description must not be blank")
    void rejectsABlankDescription(String description) {
        assertThat(violatedPaths(new FeedbackRequest(description, 8, COURSE_ID))).contains("description");
    }

    @Test
    @DisplayName("a feedback description is capped at 255 characters")
    void rejectsAnOversizedDescription() {
        assertThat(violatedPaths(new FeedbackRequest("x".repeat(256), 8, COURSE_ID)))
                .contains("description");
        assertThat(validator.validate(new FeedbackRequest("x".repeat(255), 8, COURSE_ID))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    @DisplayName("a feedback score must stay within 0..10")
    void rejectsAnOutOfRangeScore(int score) {
        assertThat(violatedPaths(new FeedbackRequest("great course", score, COURSE_ID))).contains("score");
    }

    @Test
    @DisplayName("a feedback needs both a score and a course")
    void rejectsMissingScoreAndCourse() {
        assertThat(violatedPaths(new FeedbackRequest("great course", null, null)))
                .contains("score", "courseId");
    }

    // --- CourseRequest -----------------------------------------------------------------

    @Test
    @DisplayName("a complete course payload passes validation")
    void acceptsAValidCourseRequest() {
        assertThat(validator.validate(new CourseRequest("Arquitetura", "Padroes"))).isEmpty();
    }

    @Test
    @DisplayName("a course needs a non-blank name and description")
    void rejectsABlankCourse() {
        assertThat(violatedPaths(new CourseRequest("  ", ""))).contains("name", "description");
    }

    @Test
    @DisplayName("course text fields are capped at 255 and 1000 characters")
    void rejectsOversizedCourseText() {
        assertThat(violatedPaths(new CourseRequest("x".repeat(256), "ok"))).contains("name");
        assertThat(violatedPaths(new CourseRequest("ok", "x".repeat(1001)))).contains("description");
    }

    // --- RegisterRequest ---------------------------------------------------------------

    @Test
    @DisplayName("a registration passes with only the mandatory fields")
    void acceptsAMinimalRegisterRequest() {
        assertThat(validator.validate(new RegisterRequest("Alice", "alice", "s3cret", null, null)))
                .isEmpty();
    }

    @Test
    @DisplayName("a registration requires a name, a login and a password")
    void rejectsABlankRegistration() {
        assertThat(violatedPaths(new RegisterRequest("", "  ", null, null, null)))
                .contains("name", "login", "password");
    }

    private static Set<String> violatedPaths(Object payload) {
        return validator.validate(payload).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
