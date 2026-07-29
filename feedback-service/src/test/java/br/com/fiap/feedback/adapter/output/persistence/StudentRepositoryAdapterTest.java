package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.StudentEntity;
import br.com.fiap.feedback.domain.Student;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentRepositoryAdapterTest {

    private static final UUID LOCAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private StudentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = spyAdapter();
    }

    @Test
    @DisplayName("persists a student and maps the entity back to the domain")
    void savesAStudent() {
        doNothing().when(adapter).persist(any(StudentEntity.class));

        Student saved = adapter.save(student());

        verify(adapter).persist(any(StudentEntity.class));
        assertThat(saved.getId()).isEqualTo(LOCAL_ID);
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getRegistrationNumber()).isEqualTo("RM12345");
        assertThat(saved.getAuthId()).isEqualTo(AUTH_ID);
    }

    @Test
    @DisplayName("resolves a student by their local id")
    void findsByLocalId() {
        doReturn(Optional.of(PersistenceMapper.toEntity(student())))
                .when(adapter).findByIdOptional(LOCAL_ID);

        assertThat(adapter.findByLocalId(LOCAL_ID))
                .isPresent()
                .get()
                .extracting(Student::getName)
                .isEqualTo("Alice");
    }

    @Test
    @DisplayName("returns empty when no student has that local id")
    void findsNothingByLocalId() {
        doReturn(Optional.empty()).when(adapter).findByIdOptional(LOCAL_ID);

        assertThat(adapter.findByLocalId(LOCAL_ID)).isEmpty();
    }

    @Test
    @DisplayName("resolves a student by the auth-service id carried in the JWT")
    void findsByAuthId() {
        doReturn(query(PersistenceMapper.toEntity(student()))).when(adapter).find("authId", AUTH_ID);

        assertThat(adapter.findByAuthId(AUTH_ID))
                .isPresent()
                .get()
                .extracting(Student::getId)
                .isEqualTo(LOCAL_ID);
    }

    @Test
    @DisplayName("returns empty when no student matches the auth id")
    void findsNothingByAuthId() {
        doReturn(query(null)).when(adapter).find("authId", AUTH_ID);

        assertThat(adapter.findByAuthId(AUTH_ID)).isEmpty();
    }

    private static StudentRepositoryAdapter spyAdapter() {
        return org.mockito.Mockito.spy(new StudentRepositoryAdapter());
    }

    @SuppressWarnings("unchecked")
    private static PanacheQuery<StudentEntity> query(StudentEntity result) {
        PanacheQuery<StudentEntity> query = mock(PanacheQuery.class);
        when(query.firstResultOptional()).thenReturn(Optional.ofNullable(result));
        return query;
    }

    private static Student student() {
        return Student.builder()
                .id(LOCAL_ID).name("Alice").registrationNumber("RM12345").authId(AUTH_ID).build();
    }
}
