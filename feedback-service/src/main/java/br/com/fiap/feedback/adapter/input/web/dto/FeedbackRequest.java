package br.com.fiap.feedback.adapter.input.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Feedback submission payload for {@code POST /feedback}. */
public record FeedbackRequest(
        @NotBlank String description,
        @NotNull @Min(0) @Max(10) Integer score
) {}
