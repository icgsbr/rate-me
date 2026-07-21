package br.com.fiap.feedback.adapter.output.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors the {@code application/problem+json} body (RFC 7807) returned by the auth-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthProblemDetail(
        String type,
        String title,
        Integer status,
        String detail,
        String instance
) {}
