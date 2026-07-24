package br.com.fiap.notification.adapter.input.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

/**
 * Anonymous liveness probe at {@code GET /api/health}.
 *
 * <p>Kept key-free on purpose: it exposes no data and is what the platform warm-up probe
 * and the Postman collection use to tell "the function app is up" apart from
 * "the alert endpoint rejected my key".</p>
 */
public class HealthFunction {

    @FunctionName("health")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "health")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"status\":\"UP\",\"service\":\"notification-function\"}")
                .build();
    }
}
