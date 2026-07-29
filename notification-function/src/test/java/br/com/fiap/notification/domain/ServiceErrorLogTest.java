package br.com.fiap.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceErrorLogTest {

    private static final OffsetDateTime TIMESTAMP =
            OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    @DisplayName("keeps a detail of exactly MAX_DETAIL_LENGTH characters untouched")
    void keepsDetailAtTheLimit() {
        String atLimit = "x".repeat(ServiceErrorLog.MAX_DETAIL_LENGTH);

        ServiceErrorLog error = new ServiceErrorLog(TIMESTAMP, "trace", atLimit, "op-1");

        assertThat(error.detail())
                .hasSize(ServiceErrorLog.MAX_DETAIL_LENGTH)
                .doesNotEndWith("...");
    }

    @Test
    @DisplayName("truncates a detail one character over the limit and appends an ellipsis")
    void truncatesDetailOverTheLimit() {
        String tooLong = "x".repeat(ServiceErrorLog.MAX_DETAIL_LENGTH + 1);

        ServiceErrorLog error = new ServiceErrorLog(TIMESTAMP, "trace", tooLong, "op-1");

        assertThat(error.detail())
                .hasSize(ServiceErrorLog.MAX_DETAIL_LENGTH + 3)
                .endsWith("...")
                .startsWith("x".repeat(ServiceErrorLog.MAX_DETAIL_LENGTH));
    }

    @Test
    @DisplayName("lets a null detail through without truncating")
    void allowsNullDetail() {
        ServiceErrorLog error = new ServiceErrorLog(TIMESTAMP, "exception", null, "op-2");

        assertThat(error.detail()).isNull();
        assertThat(error.kind()).isEqualTo("exception");
        assertThat(error.operationId()).isEqualTo("op-2");
        assertThat(error.timestamp()).isEqualTo(TIMESTAMP);
    }
}
