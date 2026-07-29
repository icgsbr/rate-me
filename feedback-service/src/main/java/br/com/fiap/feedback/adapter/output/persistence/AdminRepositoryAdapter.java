package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.AdminEntity;
import br.com.fiap.feedback.application.port.output.AdminRepositoryPort;
import br.com.fiap.feedback.domain.Admin;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AdminRepositoryAdapter
        implements PanacheRepositoryBase<AdminEntity, UUID>, AdminRepositoryPort {

    @Override
    public Admin save(Admin admin) {
        AdminEntity entity = PersistenceMapper.toEntity(admin);
        persist(entity);
        return PersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<Admin> findByAuthId(UUID authId) {
        return find("authId", authId).firstResultOptional().map(PersistenceMapper::toDomain);
    }
}
