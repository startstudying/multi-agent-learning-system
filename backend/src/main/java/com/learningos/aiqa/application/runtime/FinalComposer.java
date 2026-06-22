package com.learningos.aiqa.application.runtime;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaRequest;
import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.api.dto.AiQaDtos.LearnerFit;
import com.learningos.aiqa.api.dto.AiQaDtos.NextStep;
import com.learningos.aiqa.api.dto.AiQaDtos.ToolCallSummary;
import com.learningos.aiqa.api.dto.AiQaDtos.Uncertainty;
import com.learningos.aiqa.api.dto.AiQaDtos.VerificationSummary;
import com.learningos.aiqa.application.QaModePolicy;
import com.learningos.aiqa.application.memory.MemoryContext;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FinalComposer {

    public AiQaResponse compose(
            AiQaRequest request,
            QaModePolicy.QaModeStrategy strategy,
            IntentRouter.QaIntent intent,
            MemoryContext memoryContext,
            ContextOrchestrator.QaContextEnvelope contextEnvelope,
            RagQueryResponse ragResponse,
            boolean generalFallback,
            List<SourceCitation> sources,
            List<ToolCallSummary> toolCalls
    ) {
        List<SourceCitation> citations = sources == null ? List.of() : List.copyOf(sources);
        return new AiQaResponse(
                generalFallback ? buildGeneralFallbackAnswer(request.question()) : ragResponse.answer(),
                strategy.answerMode(),
                strategy.reasoningEffort(),
                strategy.safeReasoningSummary(citations.size(), generalFallback),
                generalFallback ? "GENERAL_FALLBACK" : "COURSE_GROUNDED",
                generalFallback ? "NO_COURSE_SOURCE_FALLBACK" : "COURSE_RAG",
                citations,
                citations,
                learnerFit(memoryContext, contextEnvelope, generalFallback),
                nextSteps(generalFallback, citations.size(), intent),
                uncertainty(generalFallback, citations.size()),
                qualityFlags(generalFallback, citations.size(), memoryContext),
                generalFallback || ("HIGH".equals(intent.riskLevel()) && citations.isEmpty()),
                null,
                ragResponse.traceId(),
                null,
                List.copyOf(toolCalls)
        );
    }

    public ToolCallSummary toolCallSummary(boolean generalFallback) {
        return new ToolCallSummary(
                "FinalComposer",
                "SUCCESS",
                "schema=STRUCTURED_SCHEMA_V1;sourceState=" + (generalFallback ? "NO_SOURCE" : "COURSE_GROUNDED")
        );
    }

    public AiQaResponse withVerification(
            AiQaResponse response,
            VerificationSummary verification,
            ToolCallSummary verifierToolCall
    ) {
        List<String> qualityFlags = mergedFlags(response.qualityFlags(), verification.qualityFlags());
        List<ToolCallSummary> toolCalls = new ArrayList<>(response.toolCalls() == null ? List.of() : response.toolCalls());
        toolCalls.add(verifierToolCall);
        return new AiQaResponse(
                response.answer(),
                response.answerMode(),
                response.reasoningEffort(),
                response.reasoningSummary(),
                response.sourceStatus(),
                response.sourcePolicy(),
                response.sources(),
                response.citations(),
                response.learnerFit(),
                response.nextSteps(),
                response.uncertainty(),
                qualityFlags,
                response.requiresReview() || verification.requiresReview(),
                verification,
                response.traceId(),
                response.workflowId(),
                List.copyOf(toolCalls)
        );
    }

    private List<String> mergedFlags(List<String> existing, List<String> additional) {
        List<String> flags = new ArrayList<>();
        if (existing != null) {
            existing.stream().filter(flag -> flag != null && !flag.isBlank()).forEach(flags::add);
        }
        if (additional != null) {
            additional.stream()
                    .filter(flag -> flag != null && !flag.isBlank())
                    .filter(flag -> !flags.contains(flag))
                    .forEach(flags::add);
        }
        return List.copyOf(flags);
    }

    private LearnerFit learnerFit(
            MemoryContext memoryContext,
            ContextOrchestrator.QaContextEnvelope contextEnvelope,
            boolean generalFallback
    ) {
        String profileRef = memoryContext == null || memoryContext.learner() == null
                ? "profile:none"
                : memoryContext.learner().profileRef();
        int contextItems = contextEnvelope == null ? 0 : contextEnvelope.contextItems();
        String summary = profileRef + " used with " + contextItems
                + " low-sensitive context items"
                + (generalFallback ? "; course source unavailable." : "; course source grounded.");
        double score = contextItems == 0 ? 0.20 : generalFallback ? 0.55 : 0.78;
        return new LearnerFit(summary, contextItems, score);
    }

    private List<NextStep> nextSteps(boolean generalFallback, int citationCount, IntentRouter.QaIntent intent) {
        List<NextStep> steps = new ArrayList<>();
        if (generalFallback) {
            steps.add(new NextStep("补充课程资料", "上传或选择相关知识库后重新提问。"));
            steps.add(new NextStep("缩小问题范围", "补充课程章节、概念名或具体例题。"));
        } else {
            steps.add(new NextStep("核对引用", "先查看本回答引用的 " + citationCount + " 条课程来源。"));
            steps.add(new NextStep("最小例子验证", "用一个小例子复现结论，确认边界条件。"));
        }
        if (intent != null && "HIGH".equals(intent.complexity())) {
            steps.add(new NextStep("深入追问", "要求系统按步骤拆解关键前提和例外情况。"));
        }
        return List.copyOf(steps);
    }

    private Uncertainty uncertainty(boolean generalFallback, int citationCount) {
        if (generalFallback) {
            return new Uncertainty(
                    "MEDIUM",
                    "No cited course material was available, so this is a general fallback answer.",
                    List.of("NO_COURSE_SOURCE")
            );
        }
        return new Uncertainty(
                "LOW",
                "Answer is grounded in cited course material.",
                citationCount >= 2 ? List.of() : List.of("LIMITED_CITATION_COUNT")
        );
    }

    private List<String> qualityFlags(boolean generalFallback, int citationCount, MemoryContext memoryContext) {
        List<String> flags = new ArrayList<>();
        flags.add("STRUCTURED_SCHEMA_V1");
        flags.add(generalFallback ? "NO_SOURCE_FALLBACK" : "COURSE_GROUNDED");
        if (citationCount > 0) {
            flags.add("CITATIONS_PRESENT");
        }
        if (memoryContext != null && memoryContext.contextItemCount() > 0) {
            flags.add("MEMORY_CONTEXT_USED");
        }
        if (memoryContext != null && memoryContext.budget() != null && memoryContext.budget().truncated()) {
            flags.add("MEMORY_CONTEXT_TRUNCATED");
        }
        return List.copyOf(flags);
    }

    private String buildGeneralFallbackAnswer(String question) {
        String normalizedQuestion = question == null || question.isBlank() ? "这个问题" : question.trim();
        return "暂未检索到可引用的课程资料。以下是通用解释：" + normalizedQuestion
                + " 通常需要先确认概念、条件和边界，再用一个最小例子验证结论。"
                + "如果你补充课程讲义或指定知识库，我可以再按课程资料重新给出带引用的答案。";
    }
}
