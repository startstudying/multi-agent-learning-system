package com.learningos.agent.dto;

import com.learningos.agent.domain.PromptVersion;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptVersionResponse(
        String id,
        String code,
        String version,
        String promptText,
        String status,
        Instant createdAt
) {

    public static PromptVersionResponse from(PromptVersion promptVersion) {
        return from(promptVersion, true);
    }

    public static PromptVersionResponse from(PromptVersion promptVersion, boolean includePromptText) {
        return new PromptVersionResponse(
                promptVersion.getId(),
                promptVersion.getCode(),
                promptVersion.getVersion(),
                includePromptText ? promptVersion.getPromptText() : null,
                promptVersion.getStatus(),
                promptVersion.getCreatedAt()
        );
    }
}
