package com.learningos.aiqa.application.runtime;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaRequest;
import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.application.QaModePolicy;
import com.learningos.aiqa.application.memory.MemoryContext;
import com.learningos.aiqa.application.memory.MemoryContext.ContextInjectionReason;
import com.learningos.aiqa.application.memory.MemoryContext.LearnerSummary;
import com.learningos.aiqa.application.memory.MemoryContext.TokenBudget;
import com.learningos.aiqa.application.memory.MemoryContextService;
import com.learningos.aiqa.application.quality.AnswerVerifier;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.application.RagQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaRuntimeTest {

    @Mock
    private RagQueryService ragQueryService;

    @Mock
    private MemoryContextService memoryContextService;

    private QaRuntime runtime;

    @BeforeEach
    void setUp() {
        runtime = new QaRuntime(
                ragQueryService,
                new QaModePolicy(),
                memoryContextService,
                new IntentRouter(),
                new ContextOrchestrator(),
                new FinalComposer(),
                new AnswerVerifier()
        );
    }

    @Test
    void returnsStructuredGroundedAnswerWithRuntimeToolCalls() {
        AiQaRequest request = new AiQaRequest(
                "Why does SQL JOIN duplicate rows?",
                "EXPERT",
                List.of("kb_sql"),
                "course_sql",
                5,
                "req_runtime_1"
        );
        SourceCitation citation = new SourceCitation(
                "doc_sql",
                "joins.md",
                12,
                "Join cardinality",
                "One parent row can match multiple child rows.",
                0.93
        );
        RagQueryResponse ragResponse = new RagQueryResponse(
                "JOIN duplicates usually come from one-to-many matches.",
                List.of(citation),
                "trc_runtime_1"
        );
        MemoryContext memoryContext = memoryContext();

        when(ragQueryService.queryWithRequestId(
                "alice",
                false,
                false,
                request.kbIds(),
                request.question(),
                request.topK(),
                request.requestId()
        )).thenReturn(ragResponse);
        when(memoryContextService.build("alice", "course_sql", ragResponse, "EXPERT"))
                .thenReturn(memoryContext);

        AiQaResponse response = runtime.run("alice", false, false, request);

        assertThat(response.answer()).isEqualTo("JOIN duplicates usually come from one-to-many matches.");
        assertThat(response.sourceStatus()).isEqualTo("COURSE_GROUNDED");
        assertThat(response.sources()).containsExactly(citation);
        assertThat(response.citations()).containsExactly(citation);
        assertThat(response.learnerFit().summary()).contains("profile:lpf_runtime");
        assertThat(response.learnerFit().contextItems()).isEqualTo(memoryContext.contextItemCount());
        assertThat(response.nextSteps()).hasSizeGreaterThanOrEqualTo(2)
                .allSatisfy(step -> {
                    assertThat(step.title()).isNotBlank();
                    assertThat(step.action()).isNotBlank();
                });
        assertThat(response.uncertainty().level()).isEqualTo("LOW");
        assertThat(response.uncertainty().reason()).contains("cited course material");
        assertThat(response.qualityFlags()).contains("COURSE_GROUNDED", "MEMORY_CONTEXT_USED", "STRUCTURED_SCHEMA_V1");
        assertThat(response.requiresReview()).isFalse();
        assertThat(response.verification().verdict()).isEqualTo("PASS");
        assertThat(response.verification().qualityFlags()).contains("ANSWER_VERIFIED");
        assertThat(response.toolCalls())
                .extracting("name")
                .containsExactly(
                        "IntentRouter",
                        "RagQueryService",
                        "MemoryContextService",
                        "ContextOrchestrator",
                        "FinalComposer",
                        "AnswerVerifier"
                );
        assertThat(response.toolCalls().toString())
                .doesNotContain(request.question())
                .doesNotContain("chain-of-thought")
                .doesNotContain("provider key")
                .doesNotContain("teacher_note");
    }

    @Test
    void returnsStructuredNoSourceFallbackWithoutFabricatedCitations() {
        AiQaRequest request = new AiQaRequest(
                "Explain an unknown concept.",
                "THINKING",
                List.of("kb_sql"),
                "course_sql",
                3,
                null
        );
        RagQueryResponse ragResponse = new RagQueryResponse(
                "No cited course material was found for the question: Explain an unknown concept.",
                List.of(),
                "trc_runtime_no_source"
        );
        MemoryContext memoryContext = memoryContext();

        when(ragQueryService.query(
                "alice",
                false,
                false,
                request.kbIds(),
                request.question(),
                request.topK()
        )).thenReturn(ragResponse);
        when(memoryContextService.build("alice", "course_sql", ragResponse, "THINKING"))
                .thenReturn(memoryContext);

        AiQaResponse response = runtime.run("alice", false, false, request);

        assertThat(response.sourceStatus()).isEqualTo("GENERAL_FALLBACK");
        assertThat(response.sources()).isEmpty();
        assertThat(response.citations()).isEmpty();
        assertThat(response.answer()).doesNotContain("No cited course material was found");
        assertThat(response.uncertainty().level()).isEqualTo("MEDIUM");
        assertThat(response.qualityFlags()).contains("NO_SOURCE_FALLBACK", "STRUCTURED_SCHEMA_V1");
        assertThat(response.requiresReview()).isTrue();
        assertThat(response.verification().verdict()).isEqualTo("PASS");
        assertThat(response.verification().requiresReview()).isTrue();
        assertThat(response.verification().qualityFlags()).contains("NO_SOURCE_VERIFIED");
    }

    private MemoryContext memoryContext() {
        return new MemoryContext(
                new LearnerSummary(
                        "profile:lpf_runtime",
                        "Improve SQL reasoning",
                        List.of("join cardinality"),
                        "learner_profile",
                        0.86,
                        "Use low-sensitive profile summary."
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new ContextInjectionReason(
                        "LEARNER",
                        "learner_profile",
                        0.86,
                        "Adapt explanation depth."
                )),
                new TokenBudget(1200, 180, 1020, false)
        );
    }
}
