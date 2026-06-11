package com.learningos.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ResourceGenerationRequest(
        @NotBlank String learnerId,
        @NotBlank String goalId,
        @NotBlank String pathNodeId,
        @NotEmpty List<String> resourceTypes,
        String requestId
) {
}
