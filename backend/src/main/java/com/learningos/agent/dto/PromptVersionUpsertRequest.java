package com.learningos.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptVersionUpsertRequest(
        @NotBlank(message = "Prompt code is required")
        String code,

        @NotBlank(message = "Prompt version is required")
        String version,

        @NotBlank(message = "Prompt text is required")
        String promptText,

        String status
) {
}
