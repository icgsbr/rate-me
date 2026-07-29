package br.com.fiap.notification.application.port.output;

import br.com.fiap.notification.domain.ServiceErrorLog;

import java.time.OffsetDateTime;
import java.util.List;

public interface LogQueryPort {

    List<ServiceErrorLog> findErrorsBetween(OffsetDateTime from, OffsetDateTime to);
}
