package com.learningos.aiqa.api.dto;

import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public final class AiQaDtos {

    private AiQaDtos() {
    }

    public record AiQaRequest(
            @NotBlank String question,
            String answerMode,
            @NotEmpty List<String> kbIds,
            String courseId,
            Integer topK,
            String requestId
    ) {
    }

    public record AiQaResponse(
            String answer,
            String answerMode,
            String reasoningEffort,
            String reasoningSummary,
            String sourceStatus,
            String sourcePolicy,
            List<SourceCitation> sources,
            List<SourceCitation> citations,
            LearnerFit learnerFit,
            List<NextStep> nextSteps,
            Uncertainty uncertainty,
            List<String> qualityFlags,
            boolean requiresReview,
            VerificationSummary verification,
            String traceId,
            String workflowId,
            List<ToolCallSummary> toolCalls
    ) {
    }

    public record ToolCallSummary(
            String name,
            String status,
            String summary
    ) {
    }

    public record LearnerFit(
            String summary,
            int contextItems,
            double score
    ) {
    }

    public record NextStep(
            String title,
            String action
    ) {
    }

    public record Uncertainty(
            String level,
            String reason,
            List<String> factors
    ) {
    }

    public record VerificationSummary(
            String verdict,
            List<VerificationCheck> checks,
            List<String> qualityFlags,
            boolean requiresReview,
            String gatePolicy
    ) {
    }

    public record VerificationCheck(
            String name,
            String status,
            String severity,
            String message
    ) {
    }

    public record MemorySessionResponse(
            String id,
            String courseId,
            String title,
            String status,
            double salienceScore,
            Instant decayAt,
            List<MemoryMessageResponse> messages
    ) {
    }

    public record MemoryMessageResponse(
            String id,
            String contentSummary,
            String sourcePolicy,
            boolean editable,
            double salienceScore,
            Instant decayAt,
            boolean deleted,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record MemoryMessageUpdateRequest(
            @NotBlank String summary
    ) {
    }
}
