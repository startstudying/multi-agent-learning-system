package com.learningos.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerSubmitRequest(
        @NotBlank String learnerId,
        @NotBlank String questionId,
        @NotBlank String answer,
        @NotBlank @Size(max = 120) String requestId
) {
}
