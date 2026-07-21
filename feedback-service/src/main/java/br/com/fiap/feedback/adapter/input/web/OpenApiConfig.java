package br.com.fiap.feedback.adapter.input.web;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Global OpenAPI metadata: API info and the JWT bearer scheme referenced by
 * {@code @SecurityRequirement("bearerAuth")} on the secured resources.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Feedback Service API",
                version = "1.0.0",
                description = "Student feedback API: registration (via auth-service), "
                        + "feedback submission and listing.")
)
@SecurityScheme(
        securitySchemeName = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT issued by the auth-service."
)
public class OpenApiConfig {
}
