package com.learningos.learning.dto;

import java.util.List;

public record ProfileExtractResponse(
        ProfileDraft profileDraft,
        List<String> followUpQuestions,
        String reasonSummary,
        String traceId
) {
}
