package com.learningos.aiqa.application.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.assessment.domain.WrongQuestion;
import com.learningos.assessment.repository.WrongQuestionRepository;
import com.learningos.common.privacy.MemoryPrivacyPolicy;
import com.learningos.learning.domain.LearnerProfile;
import com.learningos.learning.domain.LearningEvent;
import com.learningos.learning.domain.MasteryRecord;
import com.learningos.learning.repository.LearnerProfileRepository;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.domain.KbChatMessage;
import com.learningos.rag.repository.KbChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryContextServiceTest {

    @Mock
    private LearnerProfileRepository learnerProfileRepository;

    @Mock
    private LearningEventRepository learningEventRepository;

    @Mock
    private MasteryRecordRepository masteryRecordRepository;

    @Mock
    private WrongQuestionRepository wrongQuestionRepository;

    @Mock
    private KbChatMessageRepository kbChatMessageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MemoryContextService service;

    @BeforeEach
    void setUp() {
        service = new MemoryContextService(
                learnerProfileRepository,
                learningEventRepository,
                masteryRecordRepository,
                wrongQuestionRepository,
                kbChatMessageRepository,
                new MemoryPrivacyPolicy(),
                objectMapper
        );
    }

    @Test
    void buildsLowSensitiveContextWithSourcesScoresReasonsAndBudgetCaps() throws Exception {
        LearnerProfile profile = new LearnerProfile();
        profile.setId("lpf_context_1");
        profile.setLearnerId("alice");
        profile.setTarget("Improve SQL JOIN reasoning");
        profile.setWeakPointsJson("""
                ["join duplication", "teacher_note: assign private remedial task"]
                """);
        profile.setPreferencesJson("""
                {
                  "resourcePreference": ["worked examples", "provider_key=sk-test-secret"],
                  "pace": "step by step"
                }
                """);
        profile.setDimensionsJson("""
                {
                  "dimensions": [
                    {"name": "learning_pace", "value": "slow and visual"},
                    {"name": "teacher_note", "value": "Raw private teacher note"}
                  ]
                }
                """);

        MasteryRecord mastery = new MasteryRecord();
        mastery.setId("mst_join");
        mastery.setLearnerId("alice");
        mastery.setKnowledgePointId("kp_join");
        mastery.setMastery(0.42);
        mastery.setSourceType("ASSESSMENT");
        mastery.setSourceId("ans_1");
        mastery.setReasonSummary("Needs practice distinguishing one-to-many joins.");

        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setId("wq_join");
        wrongQuestion.setLearnerId("alice");
        wrongQuestion.setQuestionId("question_1");
        wrongQuestion.setAnswerId("answer_1");
        wrongQuestion.setGradingResultId("grade_1");
        wrongQuestion.setKnowledgePointId("kp_join");
        wrongQuestion.setScore(0.35);
        wrongQuestion.setCauseAnalysis("Confused join cardinality with filtering.");
        wrongQuestion.setResourcePushStrategy("Use a small table walkthrough.");

        LearningEvent event = new LearningEvent();
        event.setId("lev_join");
        event.setLearnerId("alice");
        event.setEventType("QA_SESSION_SUMMARY");
        event.setSubjectId("course_sql");
        event.setSummary("Recent session summarized JOIN duplicates; teacher_note should not leak.");

        String fullExcerpt = "A JOIN can duplicate rows when a parent row matches multiple child rows. "
                + "This full passage is intentionally long and should not be copied into the memory context.";
        RagQueryResponse ragResponse = new RagQueryResponse(
                "RAG answer",
                List.of(new SourceCitation("doc_sql", "joins.md", 12, "Join cardinality", fullExcerpt, 0.93)),
                "trc_memory_context"
        );

        when(learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc("alice"))
                .thenReturn(Optional.of(profile));
        when(masteryRecordRepository.findByLearnerId(eq("alice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mastery)));
        when(wrongQuestionRepository.findByLearnerId(eq("alice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(wrongQuestion)));
        when(kbChatMessageRepository.findByLearnerIdAndDeletedAtIsNullAndDecayAtAfterOrderByCreatedAtDesc(
                eq("alice"),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));
        when(learningEventRepository.findByLearnerId(eq("alice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        MemoryContext context = service.build("alice", "course_sql", ragResponse, "EXPERT");

        assertThat(context.learner().profileRef()).isEqualTo("profile:lpf_context_1");
        assertThat(context.learningSignals()).isNotEmpty()
                .allSatisfy(signal -> assertContextMetadata(signal.source(), signal.score(), signal.reason()));
        assertThat(context.citations()).hasSize(1)
                .allSatisfy(citation -> assertContextMetadata(citation.source(), citation.score(), citation.reason()));
        assertThat(context.recentSessions()).hasSize(1)
                .allSatisfy(session -> assertContextMetadata(session.source(), session.score(), session.reason()));
        assertThat(context.preferences()).isNotEmpty()
                .allSatisfy(preference -> assertContextMetadata(preference.source(), preference.score(), preference.reason()));
        assertThat(context.injectionReasons()).isNotEmpty()
                .allSatisfy(reason -> assertContextMetadata(reason.source(), reason.score(), reason.reason()));
        assertThat(context.budget().maxTokens()).isEqualTo(1200);
        assertThat(context.budget().estimatedTokens()).isLessThanOrEqualTo(1200);
        assertThat(context.budget().truncated()).isTrue();

        String serialized = objectMapper.writeValueAsString(context);
        assertThat(serialized)
                .doesNotContain("alice")
                .doesNotContain("teacher_note")
                .doesNotContain("TEACHER_NOTE")
                .doesNotContain("Raw private teacher note")
                .doesNotContain("provider_key")
                .doesNotContain("sk-test-secret")
                .doesNotContain(fullExcerpt);
    }

    @Test
    void injectsOnlyActiveSessionMemoryBeforeLearningEventFallback() {
        KbChatMessage activeMemory = new KbChatMessage();
        activeMemory.setId("memm_active");
        activeMemory.setSessionId("mems_active");
        activeMemory.setLearnerId("alice");
        activeMemory.setRole("AI_QA_SUMMARY");
        activeMemory.setContentSummary("questionHash=abc;answerLength=120;sourcePolicy=COURSE_RAG;verification=PASS");
        activeMemory.setSourcePolicy("COURSE_RAG");
        activeMemory.setSalienceScore(0.82);
        activeMemory.setDecayAt(Instant.now().plusSeconds(3600));
        activeMemory.setEditable(true);
        activeMemory.setCreatedAt(Instant.now());
        activeMemory.setUpdatedAt(Instant.now());

        when(learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc("alice"))
                .thenReturn(Optional.empty());
        when(masteryRecordRepository.findByLearnerId(eq("alice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(wrongQuestionRepository.findByLearnerId(eq("alice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(kbChatMessageRepository.findByLearnerIdAndDeletedAtIsNullAndDecayAtAfterOrderByCreatedAtDesc(
                eq("alice"),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activeMemory)));

        MemoryContext context = service.build("alice", "course_sql", null, "THINKING");

        assertThat(context.recentSessions()).hasSize(1);
        assertThat(context.recentSessions().get(0).referenceId()).isEqualTo("memm_active");
        assertThat(context.recentSessions().get(0).summary())
                .contains("questionHash=abc")
                .doesNotContain("teacher_note")
                .doesNotContain("raw prompt");
    }

    private void assertContextMetadata(String source, double score, String reason) {
        assertThat(source).isNotBlank();
        assertThat(score).isBetween(0.0, 1.0);
        assertThat(reason).isNotBlank();
    }
}
