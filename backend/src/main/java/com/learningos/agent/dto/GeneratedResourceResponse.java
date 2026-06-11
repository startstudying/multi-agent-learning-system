package com.learningos.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeneratedResourceResponse(
        String resourceId,
        String type,
        String modality,
        String title,
        String reviewStatus,
        String citationSummary,
        String markdownContent,
        String safetyStatus
) {
}
