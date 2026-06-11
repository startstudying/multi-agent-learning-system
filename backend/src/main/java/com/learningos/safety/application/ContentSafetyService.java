package com.learningos.safety.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.safety.domain.SafetyReviewStatus;
import com.learningos.safety.dto.ContentSafetyResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class ContentSafetyService {

    public ContentSafetyResult checkUserInput(String text) {
        String normalized = normalize(text);
        if (normalized.contains("exam cheating") || normalized.contains("cheat on exam")) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Content safety blocked unsafe academic request");
        }
        return approved("User input passed deterministic safety rules.");
    }

    public ContentSafetyResult reviewGeneratedContent(String content, int citationCount) {
        if (citationCount <= 0) {
            return new ContentSafetyResult(
                    SafetyReviewStatus.NEEDS_REVIEW,
                    List.of("UNGROUNDED_ACADEMIC_CONTENT"),
                    List.of("Academic content requires at least one citation before it can be trusted."),
                    "UNGROUNDED",
                    true,
                    Instant.now()
            );
        }
        return new ContentSafetyResult(
                SafetyReviewStatus.APPROVED,
                List.of("GROUNDING"),
                List.of("Content is grounded by available Course RAG citations."),
                "GROUNDED",
                true,
                Instant.now()
        );
    }

    public ContentSafetyResult reviewDraftResource(String content, boolean hasCitation) {
        if (!hasCitation) {
            return new ContentSafetyResult(
                    SafetyReviewStatus.NEEDS_REVIEW,
                    List.of("MISSING_CITATION"),
                    List.of("Draft resource must pass critic review and cite course evidence before publishing."),
                    "PARTIAL",
                    true,
                    Instant.now()
            );
        }
        return new ContentSafetyResult(
                SafetyReviewStatus.NEEDS_REVIEW,
                List.of("PENDING_CRITIC"),
                List.of("Draft is grounded but still requires critic review before publishing."),
                "GROUNDED",
                true,
                Instant.now()
        );
    }

    private ContentSafetyResult approved(String reason) {
        return new ContentSafetyResult(
                SafetyReviewStatus.APPROVED,
                List.of("INPUT_SAFETY"),
                List.of(reason),
                "NOT_APPLICABLE",
                false,
                Instant.now()
        );
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
