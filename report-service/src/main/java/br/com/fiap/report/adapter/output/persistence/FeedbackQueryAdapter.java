package br.com.fiap.report.adapter.output.persistence;

import br.com.fiap.report.application.port.output.FeedbackQueryPort;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class FeedbackQueryAdapter
        implements PanacheRepository<FeedbackReadEntity>, FeedbackQueryPort {

    @Override
    public List<FeedbackRow> findBetween(OffsetDateTime from, OffsetDateTime to) {
        return find("reviewDate between ?1 and ?2", from, to)
                .list().stream()
                .map(e -> new FeedbackRow(e.getStudentId(), e.getScore(), e.getReviewDate()))
                .toList();
    }
}
