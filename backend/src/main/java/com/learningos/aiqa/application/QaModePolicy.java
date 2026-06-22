package com.learningos.aiqa.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class QaModePolicy {

    public QaModeStrategy resolve(String answerMode) {
        String normalized = normalize(answerMode);
        return switch (normalized) {
            case "FAST" -> new QaModeStrategy("FAST", "low");
            case "THINKING" -> new QaModeStrategy("THINKING", "medium");
            case "EXPERT" -> new QaModeStrategy("EXPERT", "high");
            default -> throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported answerMode");
        };
    }

    private String normalize(String answerMode) {
        if (answerMode == null || answerMode.isBlank()) {
            return "THINKING";
        }
        return answerMode.trim().toUpperCase(Locale.ROOT);
    }

    public record QaModeStrategy(String answerMode, String reasoningEffort) {

        public String safeReasoningSummary(int citationCount, boolean generalFallback) {
            if (generalFallback) {
                return "使用 " + answerMode + " 模式和 " + reasoningEffort
                        + " reasoning effort：已先检索可访问课程资料，但未找到可靠来源；本次改为通用回答，不附加课程引用。";
            }
            return "使用 " + answerMode + " 模式和 " + reasoningEffort
                    + " reasoning effort：先检索可访问课程资料，再基于 "
                    + citationCount + " 条引用整理可展示答案。";
        }
    }
}
