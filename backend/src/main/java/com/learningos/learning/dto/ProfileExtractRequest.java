package com.learningos.learning.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileExtractRequest(
        @NotBlank String learnerId,
        ProfileUpdateSourceType sourceType,
        @NotBlank String message
) {
    public ProfileExtractRequest(String learnerId, String message) {
        this(learnerId, ProfileUpdateSourceType.CONVERSATION, message);
    }

    public ProfileUpdateSourceType sourceTypeOrDefault() {
        return sourceType == null ? ProfileUpdateSourceType.CONVERSATION : sourceType;
    }
}
