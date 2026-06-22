package com.learningos.aiqa.application.memory;

import com.learningos.aiqa.api.dto.AiQaDtos.AiQaRequest;
import com.learningos.aiqa.api.dto.AiQaDtos.AiQaResponse;
import com.learningos.aiqa.api.dto.AiQaDtos.VerificationSummary;
import com.learningos.common.privacy.MemoryPrivacyPolicy;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import com.learningos.rag.domain.KbChatMessage;
import com.learningos.rag.domain.KbChatSession;
import com.learningos.rag.repository.KbChatMessageRepository;
import com.learningos.rag.repository.KbChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryLifecycleServiceTest {

    @Mock
    private KbChatSessionRepository sessionRepository;

    @Mock
    private KbChatMessageRepository messageRepository;

    private MemoryLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new MemoryLifecycleService(sessionRepository, messageRepository, new MemoryPrivacyPolicy());
    }

    @Test
    void recordsQaTurnAsLowSensitiveEditableMemoryWithSalienceAndDecay() {
        when(sessionRepository.findById("mems_req_p2")).thenReturn(Optional.empty());
        when(sessionRepository.save(any(KbChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.save(any(KbChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KbChatMessage message = service.recordQaTurn(
                "alice",
                new AiQaRequest(
                        "teacher_note: why does SQL JOIN duplicate rows?",
                        "THINKING",
                        List.of("kb_sql"),
                        "course_sql",
                        5,
                        "req_p2"
                ),
                qaResponse("JOIN duplicates usually come from one-to-many matches.")
        );

        assertThat(message.getId()).startsWith("memm_");
        assertThat(message.getLearnerId()).isEqualTo("alice");
        assertThat(message.getSessionId()).isEqualTo("mems_req_p2");
        assertThat(message.getRole()).isEqualTo("AI_QA_SUMMARY");
        assertThat(message.getSourcePolicy()).isEqualTo("COURSE_RAG");
        assertThat(message.getSalienceScore()).isBetween(0.0, 1.0);
        assertThat(message.getSalienceScore()).isGreaterThan(0.70);
        assertThat(message.getDecayAt()).isAfter(Instant.now());
        assertThat(message.isEditable()).isTrue();

        assertThat(message.getContentSummary())
                .contains("questionHash=")
                .contains("questionLength=")
                .contains("answerLength=")
                .contains("sourcePolicy=COURSE_RAG")
                .contains("verification=PASS")
                .doesNotContain("teacher_note")
                .doesNotContain("why does SQL JOIN duplicate rows")
                .doesNotContain("JOIN duplicates usually come from one-to-many matches");
    }

    @Test
    void listsOwnerSessionsWithOnlyActiveMessages() {
        KbChatSession session = new KbChatSession();
        session.setId("mems_1");
        session.setLearnerId("alice");
        session.setCourseId("course_sql");
        session.setTitle("AI QA COURSE_RAG");
        session.setStatus("ACTIVE");
        session.setSalienceScore(0.82);
        session.setDecayAt(Instant.now().plusSeconds(3600));

        KbChatMessage message = new KbChatMessage();
        message.setId("memm_1");
        message.setSessionId("mems_1");
        message.setLearnerId("alice");
        message.setRole("AI_QA_SUMMARY");
        message.setContentSummary("questionHash=abc;answerLength=120;sourcePolicy=COURSE_RAG;verification=PASS");
        message.setSourcePolicy("COURSE_RAG");
        message.setSalienceScore(0.82);
        message.setDecayAt(Instant.now().plusSeconds(3600));
        message.setEditable(true);

        when(sessionRepository.findByLearnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(session)));
        when(messageRepository.findBySessionIdInAndLearnerIdAndDeletedAtIsNullAndDecayAtAfterOrderByCreatedAtAsc(
                any(),
                any(),
                any(Instant.class)
        )).thenReturn(List.of(message));

        var sessions = service.listSessions("alice");

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).id()).isEqualTo("mems_1");
        assertThat(sessions.get(0).messages()).hasSize(1);
        assertThat(sessions.get(0).messages().get(0).contentSummary())
                .contains("questionHash=abc")
                .doesNotContain("teacher_note");
    }

    @Test
    void updatesAndDeletesOnlyOwnerEditableMemoryMessages() {
        KbChatMessage existing = new KbChatMessage();
        existing.setId("memm_1");
        existing.setLearnerId("alice");
        existing.setSessionId("mems_1");
        existing.setRole("AI_QA_SUMMARY");
        existing.setEditable(true);
        existing.setContentSummary("old summary");
        existing.setSourcePolicy("COURSE_RAG");
        existing.setSalienceScore(0.75);
        existing.setDecayAt(Instant.now().plusSeconds(3600));
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(messageRepository.findByIdAndLearnerIdAndDeletedAtIsNull("memm_1", "alice"))
                .thenReturn(Optional.of(existing));
        when(messageRepository.save(any(KbChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KbChatMessage updated = service.updateMessageSummary(
                "alice",
                "memm_1",
                "Prefer diagrams, provider_key=sk-secret should be removed."
        );

        assertThat(updated.getContentSummary())
                .contains("[redacted-sensitive-memory]")
                .doesNotContain("provider_key")
                .doesNotContain("sk-secret");

        KbChatMessage deleted = service.deleteMessage("alice", "memm_1");

        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getUpdatedAt()).isNotNull();
    }

    private AiQaResponse qaResponse(String answer) {
        return new AiQaResponse(
                answer,
                "THINKING",
                "medium",
                "Safe summary",
                "COURSE_GROUNDED",
                "COURSE_RAG",
                List.of(new SourceCitation("doc_sql", "joins.md", 12, "Join cardinality", "One parent row can match multiple child rows.", 0.93)),
                List.of(new SourceCitation("doc_sql", "joins.md", 12, "Join cardinality", "One parent row can match multiple child rows.", 0.93)),
                null,
                List.of(),
                null,
                List.of("STRUCTURED_SCHEMA_V1"),
                false,
                new VerificationSummary("PASS", List.of(), List.of("QA_VERIFIED"), false, "BASIC_QA_VERIFIER_V1"),
                "trc_p2",
                null,
                List.of()
        );
    }
}
