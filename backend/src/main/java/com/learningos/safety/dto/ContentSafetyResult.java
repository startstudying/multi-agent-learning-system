package com.learningos.safety.dto;

import com.learningos.safety.domain.SafetyReviewStatus;

import java.time.Instant;
import java.util.List;

public record ContentSafetyResult(
        SafetyReviewStatus status,
        List<String> categories,
        List<String> reasons,
        String groundingStatus,
        boolean citationRequired,
        Instant checkedAt
) {
}
