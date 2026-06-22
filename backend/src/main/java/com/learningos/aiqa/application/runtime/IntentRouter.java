package com.learningos.aiqa.application.runtime;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaRequest;
import com.learningos.aiqa.application.QaModePolicy;
import org.springframework.stereotype.Component;

@Component
public class IntentRouter {

    public QaIntent classify(AiQaRequest request, QaModePolicy.QaModeStrategy strategy) {
        String answerMode = strategy.answerMode();
        String complexity = switch (answerMode) {
            case "FAST" -> "LOW";
            case "EXPERT" -> "HIGH";
            default -> "MEDIUM";
        };
        String qualityPolicy = switch (answerMode) {
            case "FAST" -> "BASIC_SOURCE_POLICY";
            case "EXPERT" -> "STRICT_SOURCE_POLICY";
            default -> "STANDARD_SOURCE_POLICY";
        };
        boolean needsRag = request != null && request.kbIds() != null && !request.kbIds().isEmpty();
        return new QaIntent(
                "QA",
                complexity,
                needsRag,
                true,
                qualityPolicy,
                "HIGH".equals(complexity) ? "MEDIUM" : "LOW"
        );
    }

    public record QaIntent(
            String taskType,
            String complexity,
            boolean needsRag,
            boolean needsMemory,
            String qualityPolicy,
            String riskLevel
    ) {
        public String summary() {
            return "taskType=" + taskType
                    + ";complexity=" + complexity
                    + ";needsRag=" + needsRag
                    + ";needsMemory=" + needsMemory
                    + ";qualityPolicy=" + qualityPolicy
                    + ";riskLevel=" + riskLevel;
        }
    }
}
