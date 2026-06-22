package com.learningos.aiqa.application.quality;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.api.dto.AiQaDtos.LearnerFit;
import com.learningos.aiqa.api.dto.AiQaDtos.NextStep;
import com.learningos.aiqa.api.dto.AiQaDtos.ToolCallSummary;
import com.learningos.aiqa.api.dto.AiQaDtos.Uncertainty;
import com.learningos.aiqa.api.dto.AiQaDtos.VerificationSummary;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerVerifierTest {

    private final AnswerVerifier verifier = new AnswerVerifier();

    @Test
    void passesGroundedAnswerWithConsistentCitationsAndSafeSchema() {
        VerificationSummary verification = verifier.verify(groundedResponse());

        assertThat(verification.verdict()).isEqualTo("PASS");
        assertThat(verification.requiresReview()).isFalse();
        assertThat(verification.qualityFlags()).contains("ANSWER_VERIFIED");
        assertThat(verification.checks())
                .allSatisfy(check -> assertThat(check.status()).isEqualTo("PASS"));
    }

    @Test
    void passesCompliantNoSourceFallbackButKeepsReviewRequired() {
        VerificationSummary verification = verifier.verify(noSourceResponse());

        assertThat(verification.verdict()).isEqualTo("PASS");
        assertThat(verification.requiresReview()).isTrue();
        assertThat(verification.qualityFlags()).contains("NO_SOURCE_VERIFIED");
    }

    @Test
    void failsWhenPrivateTeacherNoteLeaksIntoVisibleResponse() {
        AiQaResponse response = groundedResponseWithAnswer("teacher_note: private remedial plan");

        VerificationSummary verification = verifier.verify(response);

        assertThat(verification.verdict()).isEqualTo("FAIL");
        assertThat(verification.requiresReview()).isTrue();
        assertThat(verification.checks())
                .anySatisfy(check -> {
                    assertThat(check.name()).isEqualTo("PRIVACY_LEAK_GUARD");
                    assertThat(check.status()).isEqualTo("FAIL");
                });
    }

    @Test
    void failsWhenSourcesAndCitationsDiverge() {
        SourceCitation source = citation("doc_sql");
        AiQaResponse response = new AiQaResponse(
                "Grounded answer.",
                "THINKING",
                "medium",
                "safe summary",
                "COURSE_GROUNDED",
                "COURSE_RAG",
                List.of(source),
                List.of(citation("doc_other")),
                learnerFit(),
                nextSteps(),
                new Uncertainty("LOW", "Answer is grounded in cited course material.", List.of()),
                List.of("STRUCTURED_SCHEMA_V1", "COURSE_GROUNDED"),
                false,
                null,
                "trace_verifier",
                null,
                List.of(new ToolCallSummary("FinalComposer", "SUCCESS", "safe summary"))
        );

        VerificationSummary verification = verifier.verify(response);

        assertThat(verification.verdict()).isEqualTo("FAIL");
        assertThat(verification.checks())
                .anySatisfy(check -> {
                    assertThat(check.name()).isEqualTo("CITATION_CONSISTENCY");
                    assertThat(check.status()).isEqualTo("FAIL");
                });
    }

    @Test
    void warnsAndRequiresReviewWhenCoreClaimIsNotCoveredByCitations() {
        SourceCitation citation = new SourceCitation(
                "doc_isolation",
                "tx.md",
                1,
                "Isolation",
                "Isolation controls concurrent visibility.",
                0.95
        );
        AiQaResponse response = new AiQaResponse(
                "Isolation controls concurrent visibility. Phantom reads happen when range queries observe new rows.",
                "EXPERT",
                "high",
                "safe summary",
                "COURSE_GROUNDED",
                "COURSE_RAG",
                List.of(citation),
                List.of(citation),
                learnerFit(),
                nextSteps(),
                new Uncertainty("LOW", "Answer is grounded in cited course material.", List.of()),
                List.of("STRUCTURED_SCHEMA_V1", "COURSE_GROUNDED", "CITATIONS_PRESENT"),
                false,
                null,
                "trace_verifier",
                null,
                List.of(new ToolCallSummary("FinalComposer", "SUCCESS", "safe summary"))
        );

        VerificationSummary verification = verifier.verify(response);

        assertThat(verification.verdict()).isEqualTo("PASS");
        assertThat(verification.requiresReview()).isTrue();
        assertThat(verification.qualityFlags()).contains("CORE_CLAIM_CITATION_REVIEW", "UNCITED_CONTEXT_LEAK_REVIEW");
        assertThat(verification.checks())
                .anySatisfy(check -> {
                    assertThat(check.name()).isEqualTo("CORE_CLAIM_CITATION_COVERAGE");
                    assertThat(check.status()).isEqualTo("WARN");
                })
                .anySatisfy(check -> {
                    assertThat(check.name()).isEqualTo("UNCITED_CONTEXT_LEAK_GUARD");
                    assertThat(check.status()).isEqualTo("WARN");
                });
    }

    private AiQaResponse groundedResponse() {
        return groundedResponseWithAnswer("JOIN duplicates usually come from one-to-many matches.");
    }

    private AiQaResponse groundedResponseWithAnswer(String answer) {
        SourceCitation citation = citation("doc_sql");
        return new AiQaResponse(
                answer,
                "EXPERT",
                "high",
                "safe summary",
                "COURSE_GROUNDED",
                "COURSE_RAG",
                List.of(citation),
                List.of(citation),
                learnerFit(),
                nextSteps(),
                new Uncertainty("LOW", "Answer is grounded in cited course material.", List.of()),
                List.of("STRUCTURED_SCHEMA_V1", "COURSE_GROUNDED", "MEMORY_CONTEXT_USED"),
                false,
                null,
                "trace_verifier",
                null,
                List.of(new ToolCallSummary("FinalComposer", "SUCCESS", "safe summary"))
        );
    }

    private AiQaResponse noSourceResponse() {
        return new AiQaResponse(
                "暂未检索到可引用的课程资料。以下是通用解释：示例问题",
                "THINKING",
                "medium",
                "safe summary",
                "GENERAL_FALLBACK",
                "NO_COURSE_SOURCE_FALLBACK",
                List.of(),
                List.of(),
                learnerFit(),
                nextSteps(),
                new Uncertainty("MEDIUM", "No cited course material was available.", List.of("NO_COURSE_SOURCE")),
                List.of("STRUCTURED_SCHEMA_V1", "NO_SOURCE_FALLBACK"),
                true,
                null,
                "trace_no_source",
                null,
                List.of(new ToolCallSummary("FinalComposer", "SUCCESS", "safe summary"))
        );
    }

    private SourceCitation citation(String documentId) {
        return new SourceCitation(documentId, documentId + ".md", 1, "section", "excerpt", 0.95);
    }

    private LearnerFit learnerFit() {
        return new LearnerFit("profile:lpf_safe used with low-sensitive context", 1, 0.78);
    }

    private List<NextStep> nextSteps() {
        return List.of(new NextStep("核对引用", "查看课程来源。"));
    }
}
