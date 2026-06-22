package com.learningos.aiqa.application.runtime;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaRequest;
import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.api.dto.AiQaDtos.ToolCallSummary;
import com.learningos.aiqa.api.dto.AiQaDtos.VerificationSummary;
import com.learningos.aiqa.application.QaModePolicy;
import com.learningos.aiqa.application.memory.MemoryContext;
import com.learningos.aiqa.application.memory.MemoryContextService;
import com.learningos.aiqa.application.quality.AnswerVerifier;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.application.RagQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QaRuntime {

    private final RagQueryService ragQueryService;
    private final QaModePolicy qaModePolicy;
    private final MemoryContextService memoryContextService;
    private final IntentRouter intentRouter;
    private final ContextOrchestrator contextOrchestrator;
    private final FinalComposer finalComposer;
    private final AnswerVerifier answerVerifier;

    public QaRuntime(
            RagQueryService ragQueryService,
            QaModePolicy qaModePolicy,
            MemoryContextService memoryContextService,
            IntentRouter intentRouter,
            ContextOrchestrator contextOrchestrator,
            FinalComposer finalComposer,
            AnswerVerifier answerVerifier
    ) {
        this.ragQueryService = ragQueryService;
        this.qaModePolicy = qaModePolicy;
        this.memoryContextService = memoryContextService;
        this.intentRouter = intentRouter;
        this.contextOrchestrator = contextOrchestrator;
        this.finalComposer = finalComposer;
        this.answerVerifier = answerVerifier;
    }

    public AiQaResponse run(String userId, boolean admin, boolean teacher, AiQaRequest request) {
        QaModePolicy.QaModeStrategy strategy = qaModePolicy.resolve(request.answerMode());
        IntentRouter.QaIntent intent = intentRouter.classify(request, strategy);
        List<ToolCallSummary> toolCalls = new ArrayList<>();
        toolCalls.add(new ToolCallSummary("IntentRouter", "SUCCESS", intent.summary()));

        RagQueryResponse ragResponse = hasText(request.requestId())
                ? ragQueryService.queryWithRequestId(
                userId,
                admin,
                teacher,
                request.kbIds(),
                request.question(),
                request.topK(),
                request.requestId()
        )
                : ragQueryService.query(
                userId,
                admin,
                teacher,
                request.kbIds(),
                request.question(),
                request.topK()
        );

        int citationCount = ragResponse.sources() == null ? 0 : ragResponse.sources().size();
        boolean generalFallback = isNoSource(ragResponse, citationCount);
        List<SourceCitation> sources = generalFallback
                ? List.of()
                : ragResponse.sources() == null ? List.of() : ragResponse.sources();
        toolCalls.add(new ToolCallSummary(
                "RagQueryService",
                "SUCCESS",
                "sourceState=" + (generalFallback ? "NO_SOURCE" : "COURSE_GROUNDED")
                        + ";citations=" + sources.size()
                        + ";traceId=" + nullToBlank(ragResponse.traceId())
        ));

        MemoryContext memoryContext = memoryContextService.build(
                userId,
                request.courseId(),
                ragResponse,
                strategy.answerMode()
        );
        toolCalls.add(new ToolCallSummary("MemoryContextService", "SUCCESS", memoryContext.summary()));

        ContextOrchestrator.QaContextEnvelope contextEnvelope = contextOrchestrator.build(
                intent,
                memoryContext,
                ragResponse,
                generalFallback
        );
        toolCalls.add(new ToolCallSummary("ContextOrchestrator", "SUCCESS", contextEnvelope.summary()));
        toolCalls.add(finalComposer.toolCallSummary(generalFallback));

        AiQaResponse response = finalComposer.compose(
                request,
                strategy,
                intent,
                memoryContext,
                contextEnvelope,
                ragResponse,
                generalFallback,
                sources,
                toolCalls
        );
        VerificationSummary verification = answerVerifier.verify(response);
        return finalComposer.withVerification(response, verification, answerVerifier.toolCallSummary(verification));
    }

    private boolean isNoSource(RagQueryResponse ragResponse, int citationCount) {
        return citationCount == 0 || (ragResponse.retrieval() != null && ragResponse.retrieval().noSource());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
