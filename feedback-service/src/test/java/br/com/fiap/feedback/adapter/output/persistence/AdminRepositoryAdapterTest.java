package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.AdminEntity;
import br.com.fiap.feedback.domain.Admin;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminRepositoryAdapterTest {

    private static final UUID LOCAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private AdminRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = spy(new AdminRepositoryAdapter());
    }

    @Test
    @DisplayName("persists an admin and maps the entity back to the domain")
    void savesAnAdmin() {
        doNothing().when(adapter).persist(any(AdminEntity.class));

        Admin saved = adapter.save(admin());

        verify(adapter).persist(any(AdminEntity.class));
        assertThat(saved.getId()).isEqualTo(LOCAL_ID);
        assertThat(saved.getName()).isEqualTo("Root");
        assertThat(saved.getAuthId()).isEqualTo(AUTH_ID);
        assertThat(saved.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("resolves an admin by the auth-service id carried in the JWT")
    void findsByAuthId() {
        doReturn(query(PersistenceMapper.toEntity(admin()))).when(adapter).find("authId", AUTH_ID);

        assertThat(adapter.findByAuthId(AUTH_ID))
                .isPresent()
                .get()
                .extracting(Admin::getRole)
                .isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("returns empty when the caller is not an admin")
    void findsNothingByAuthId() {
        doReturn(query(null)).when(adapter).find("authId", AUTH_ID);

        assertThat(adapter.findByAuthId(AUTH_ID)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static PanacheQuery<AdminEntity> query(AdminEntity result) {
        PanacheQuery<AdminEntity> query = mock(PanacheQuery.class);
        when(query.firstResultOptional()).thenReturn(Optional.ofNullable(result));
        return query;
    }

    private static Admin admin() {
        return Admin.builder().id(LOCAL_ID).name("Root").authId(AUTH_ID).role("ADMIN").build();
    }
}
