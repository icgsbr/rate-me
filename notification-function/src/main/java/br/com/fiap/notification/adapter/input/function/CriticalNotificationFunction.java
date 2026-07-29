package br.com.fiap.notification.adapter.input.function;

import br.com.fiap.notification.adapter.input.function.dto.CriticalNotificationRequest;
import br.com.fiap.notification.adapter.input.function.dto.CriticalNotificationResponse;
import br.com.fiap.notification.adapter.input.function.dto.ErrorResponse;
import br.com.fiap.notification.application.port.input.SendCriticalNotificationUseCase;
import br.com.fiap.notification.domain.CriticalNotification;
import br.com.fiap.notification.domain.InvalidNotificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CriticalNotificationFunction {

    private static final Logger log = LoggerFactory.getLogger(CriticalNotificationFunction.class);

    @Inject
    SendCriticalNotificationUseCase sendNotification;

    @Inject
    ObjectMapper objectMapper;

    @FunctionName("notifyCritical")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "notifications/critical")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        String rawBody = request.getBody().orElse("");
        if (rawBody.isBlank()) {
            return badRequest(request, "a JSON body is required");
        }

        CriticalNotification notification;
        try {
            notification = objectMapper.readValue(rawBody, CriticalNotificationRequest.class).toDomain();
        } catch (InvalidNotificationException e) {
            return badRequest(request, e.getMessage());
        } catch (Exception e) {
            return badRequest(request, "malformed JSON body: " + e.getMessage());
        }

        try {
            sendNotification.send(notification);
        } catch (Exception e) {
            log.error("Failed to deliver critical alert", e);
            return json(request, HttpStatus.INTERNAL_SERVER_ERROR,
                    new ErrorResponse("delivery_failed", e.getMessage()));
        }

        return json(request, HttpStatus.OK, CriticalNotificationResponse.sent(notification));
    }

    private HttpResponseMessage badRequest(HttpRequestMessage<Optional<String>> request, String message) {
        log.warn("Rejected critical alert: {}", message);
        return json(request, HttpStatus.BAD_REQUEST, new ErrorResponse("invalid_payload", message));
    }

    private HttpResponseMessage json(HttpRequestMessage<Optional<String>> request,
                                     HttpStatus status,
                                     Object body) {
        try {
            return request.createResponseBuilder(status)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(body))
                    .build();
        } catch (Exception e) {
            log.error("Failed to serialize response", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"serialization_failed\"}")
                    .build();
        }
    }
}
