package com.learningos.learning.dto;

public record LearningPathNodeResponse(
        String nodeId,
        String title,
        String status,
        double mastery,
        String reasonSummary,
        String recommendationReason,
        int estimatedDurationMinutes,
        String resourceType,
        String assessmentBindingRelation
) {
}
