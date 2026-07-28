package br.com.fiap.notification.application.port.input;

import java.time.OffsetDateTime;

public interface ScanServiceErrorsUseCase {

    int scan(OffsetDateTime from, OffsetDateTime to);
}
