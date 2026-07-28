package br.com.fiap.feedback.adapter.output.persistence;

import br.com.fiap.feedback.adapter.output.persistence.entity.FeedbackEntity;
import br.com.fiap.feedback.application.port.output.FeedbackRepositoryPort;
import br.com.fiap.feedback.domain.Feedback;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FeedbackRepositoryAdapter
        implements PanacheRepository<FeedbackEntity>, FeedbackRepositoryPort {

    @Override
    public Feedback save(Feedback feedback) {
        if (feedback.getId() == null) {
            FeedbackEntity entity = PersistenceMapper.toEntity(feedback);
            persist(entity);
            return PersistenceMapper.toDomain(entity);
        }
        FeedbackEntity entity = findById(feedback.getId());
        entity.setNotified(feedback.isNotified());
        entity.setNotifiedDate(feedback.getNotifiedDate());
        return PersistenceMapper.toDomain(entity);
    }

    @Override
    public long countAll() {
        return count();
    }

    @Override
    public List<Feedback> findByStudentId(UUID studentId) {
        return find("studentId", Sort.by("reviewDate").descending(), studentId)
                .list().stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Feedback> findAll(int page, int size) {
        return findAll(Sort.by("reviewDate").descending())
                .page(Page.of(page, size))
                .list().stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
