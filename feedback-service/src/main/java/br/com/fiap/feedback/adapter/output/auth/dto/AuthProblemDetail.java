package br.com.fiap.feedback.adapter.output.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthProblemDetail(
        String type,
        String title,
        Integer status,
        String detail,
        String instance
) {}
