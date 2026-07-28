package br.com.fiap.notification.adapter.input.function;

import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthFunctionTest {

    @Test
    @DisplayName("answers 200 with the service identity so the probe can tell instances apart")
    void reportsUp() {
        HttpResponseMessage response = new HealthFunction().run(FakeHttpRequestMessage.withoutBody(), null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(response.getBody())
                .isEqualTo("{\"status\":\"UP\",\"service\":\"notification-function\"}");
    }
}
