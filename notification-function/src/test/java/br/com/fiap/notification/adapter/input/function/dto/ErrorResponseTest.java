package br.com.fiap.notification.adapter.input.function.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    @DisplayName("serialises to the error/message shape the clients expect")
    void serialisesToJson() throws Exception {
        String json = new ObjectMapper()
                .writeValueAsString(new ErrorResponse("invalid_payload", "a JSON body is required"));

        assertThat(json).isEqualTo(
                "{\"error\":\"invalid_payload\",\"message\":\"a JSON body is required\"}");
    }
}
