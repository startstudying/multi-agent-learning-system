package com.learningos.learning.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLearningPathRequest(
        @NotBlank String learnerId,
        @NotBlank String goalId
) {
}
