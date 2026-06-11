package com.learningos.agent.dto;

public record LearnerResourceResponse(
        String resourceId,
        String type,
        String modality,
        String title,
        String citationSummary,
        String markdownContent
) {
}
