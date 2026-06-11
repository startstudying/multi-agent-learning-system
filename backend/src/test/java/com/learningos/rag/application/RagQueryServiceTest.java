package com.learningos.rag.application;

import com.learningos.common.exception.ApiException;
import com.learningos.common.api.ErrorCode;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.DocumentStatus;
import com.learningos.rag.domain.enums.KnowledgeBaseBindingStatus;
import com.learningos.rag.domain.enums.Visibility;
import com.learningos.rag.repository.KbDocChunkRepository;
import com.learningos.rag.repository.KbDocumentRepository;
import com.learningos.rag.repository.KbQueryLogRepository;
import com.learningos.rag.repository.KnowledgeBaseRepository;
import com.learningos.rag.repository.SourceCitationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.rag.reranker-timeout-ms=10"
})
class RagQueryServiceTest {

    private final RagQueryService ragQueryService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocumentRepository documentRepository;
    private final KbDocChunkRepository chunkRepository;
    private final KbQueryLogRepository queryLogRepository;
    private final SourceCitationRepository sourceCitationRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final MeterRegistry meterRegistry;
    private final TestRerankerService testRerankerService;

    RagQueryServiceTest(
            RagQueryService ragQueryService,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KbDocumentRepository documentRepository,
            KbDocChunkRepository chunkRepository,
            KbQueryLogRepository queryLogRepository,
            SourceCitationRepository sourceCitationRepository,
            CourseRepository courseRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            MeterRegistry meterRegistry,
            TestRerankerService testRerankerService
    ) {
        this.ragQueryService = ragQueryService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.queryLogRepository = queryLogRepository;
        this.sourceCitationRepository = sourceCitationRepository;
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.meterRegistry = meterRegistry;
        this.testRerankerService = testRerankerService;
    }

    @BeforeEach
    void resetReranker() {
        testRerankerService.reset();
    }

    @Test
    void returnsAnswerSourcesAndTraceIdFromAllowedChunks() {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");
        long beforeDurationCount = ragDurationCount("COURSE_RAG", "success", "false", "false", "OK");

        var response = ragQueryService.query("alice", List.of("kb_sql"), "Why does SQL JOIN duplicate rows?", 5);

        assertThat(response.answer()).contains("SQL JOIN duplicates");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().getFirst().documentId()).isEqualTo("doc_sql");
        assertThat(response.retrieval()).isNotNull();
        assertThat(response.retrieval().strategy()).isEqualTo("COURSE_RAG");
        assertThat(response.retrieval().queryComplexity()).isEqualTo("COMPLEX");
        assertThat(response.retrieval().noSource()).isFalse();
        assertThat(response.retrieval().retrievalCount()).isEqualTo(1);
        assertThat(response.retrieval().candidateCount()).isEqualTo(1);
        assertThat(response.retrieval().citationCount()).isEqualTo(1);
        assertThat(response.retrieval().downgraded()).isFalse();
        assertThat(response.retrieval().message()).contains("Retrieved 1 cited course chunk");
        assertThat(response.traceId()).isNotBlank();
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("retrievalCount")).isEqualTo(1);
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("NOT_CONFIGURED");
        assertThat((String) logFields.getPropertyValue("sourcesJson"))
                .contains("\"strategy\":\"COURSE_RAG\"")
                .contains("\"retrievalMode\":\"HYBRID_RRF\"")
                .contains("\"vectorEnabled\":false")
                .contains("\"status\":\"NOT_CONFIGURED\"")
                .contains("\"candidateCount\":1")
                .contains("\"citationCount\":1")
                .contains("\"downgraded\":false");
        assertThat(meterRegistry.find("learningos.rag.query.duration")
                .tag("strategy", "COURSE_RAG")
                .tag("outcome", "success")
                .tag("no_source", "false")
                .tag("replayed", "false")
                .tag("error_code", "OK")
                .timer()).isNotNull();
        assertThat(ragDurationCount("COURSE_RAG", "success", "false", "false", "OK"))
                .isEqualTo(beforeDurationCount + 1);
        assertThat(meterRegistry.find("learningos.rag.retrieval.count")
                .tag("strategy", "COURSE_RAG")
                .tag("no_source", "false")
                .summary()
                .totalAmount()).isGreaterThanOrEqualTo(1.0);
        assertNoSensitiveMetricTags();
    }

    @Test
    void prefersKeywordRelevantChunkWhenQueryTermsMatchOnlyOneCandidate() {
        Instant now = Instant.now();
        seedIndexedChunk(
                "kb_sql",
                "doc_noise",
                "Graph traversal basics and breadth first search.",
                "chunk_noise",
                now,
                "Graph traversal"
        );
        seedIndexedChunk(
                "kb_sql",
                "doc_join",
                "SQL JOIN duplicates come from one-to-many cardinality.",
                "chunk_join",
                now.minusSeconds(3600),
                "Multi table joins"
        );

        var response = ragQueryService.query("alice", List.of("kb_sql"), "Why does SQL JOIN duplicate rows?", 1);

        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().getFirst().documentId()).isEqualTo("doc_join");
        assertThat(response.answer()).contains("SQL JOIN duplicates");
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("NOT_CONFIGURED");
        assertThat((String) logFields.getPropertyValue("sourcesJson"))
                .contains("\"retrievalMode\":\"HYBRID_RRF\"")
                .contains("\"keywordCandidateCount\":1")
                .contains("\"vectorCandidateCount\":0")
                .contains("\"vectorEnabled\":false")
                .contains("\"fusedCandidateCount\":1")
                .contains("\"status\":\"NOT_CONFIGURED\"");
    }

    @Test
    void rerankerTimeoutFallsBackToFusedCandidatesAndMarksDowngraded() {
        testRerankerService.timeout();
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        var response = ragQueryService.query("alice", List.of("kb_sql"), "Why does SQL JOIN duplicate rows?", 5);

        assertThat(response.sources()).hasSize(1);
        assertThat(response.retrieval().strategy()).isEqualTo("COURSE_RAG");
        assertThat(response.retrieval().downgraded()).isTrue();
        assertThat(response.retrieval().message()).contains("TIMEOUT_FALLBACK");
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("TIMEOUT_FALLBACK");
        assertThat((String) logFields.getPropertyValue("sourcesJson"))
                .contains("\"status\":\"TIMEOUT_FALLBACK\"")
                .contains("\"fallbackUsed\":true")
                .doesNotContain("TimeoutException", "rawPrompt", "apiKey", "secret");
    }

    @Test
    void rerankerProviderErrorIsSanitizedAndFallsBackToFusedCandidates() {
        testRerankerService.failWithRawMessage();
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        var response = ragQueryService.query("alice", List.of("kb_sql"), "Why does SQL JOIN duplicate rows?", 5);

        assertThat(response.sources()).hasSize(1);
        assertThat(response.retrieval().downgraded()).isTrue();
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("ERROR_FALLBACK");
        assertThat((String) logFields.getPropertyValue("sourcesJson"))
                .contains("\"status\":\"ERROR_FALLBACK\"")
                .contains("\"errorCode\":\"RERANKER_ERROR\"")
                .doesNotContain("sk-test", "raw chunk", "provider said");
    }

    @Test
    void queryWithTraceIdPersistsProvidedTraceId() {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        var response = ragQueryService.queryWithTraceId(
                "alice",
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "trc_orchestrated_rag"
        );

        assertThat(response.traceId()).isEqualTo("trc_orchestrated_rag");
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("traceId")).isEqualTo("trc_orchestrated_rag");
        assertThat(sourceCitationRepository.findAll().getFirst().getTraceId()).isEqualTo("trc_orchestrated_rag");
    }

    @Test
    void queryWithRequestIdPersistsRequestHashAndResponseSnapshot() {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        var response = ragQueryService.queryWithTraceIdAndRequestId(
                "alice",
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "trc_rag_snapshot",
                "req_rag_snapshot"
        );

        assertThat(response.traceId()).isEqualTo("trc_rag_snapshot");
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("requestId")).isEqualTo("req_rag_snapshot");
        assertThat((String) logFields.getPropertyValue("requestHash")).hasSize(64);
        assertThat((String) logFields.getPropertyValue("responseJson"))
                .contains("\"traceId\":\"trc_rag_snapshot\"")
                .contains("\"strategy\":\"COURSE_RAG\"")
                .contains("\"sources\"");
    }

    @Test
    void roleAwareAdminQueryCanReadForeignPrivateKnowledgeBase() {
        seedPrivateKnowledgeBase("kb_foreign", "bob");
        seedIndexedChunk("kb_foreign", "doc_foreign", "Hidden admin-only material.");

        var response = ragQueryService.query(
                "ops_admin",
                true,
                false,
                List.of("kb_foreign"),
                "What is hidden?",
                5
        );

        assertThat(response.answer()).contains("Hidden admin-only material");
        assertThat(response.sources()).hasSize(1);
        assertThat(queryLogRepository.count()).isEqualTo(1);
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("userId")).isEqualTo("ops_admin");
    }

    @Test
    void studentCanQueryCourseBoundKnowledgeBaseWhenActivelyEnrolled() {
        seedCourse("course_sql", "instructor_1");
        seedEnrollment("course_sql", "student_active", "ACTIVE");
        seedBoundKnowledgeBase("kb_course_sql", "instructor_1", "course_sql", Visibility.PRIVATE);
        seedIndexedChunk("kb_course_sql", "doc_course_sql", "Course-bound SQL JOIN material.");

        var response = ragQueryService.query(
                "student_active",
                false,
                false,
                List.of("kb_course_sql"),
                "What does the course say about SQL JOIN?",
                5
        );

        assertThat(response.answer()).contains("Course-bound SQL JOIN material");
        assertThat(response.sources()).hasSize(1);
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
    }

    @Test
    void droppedStudentCannotQueryCourseBoundPublicKnowledgeBaseOrPersistArtifacts() {
        seedCourse("course_sql", "instructor_1");
        seedEnrollment("course_sql", "student_dropped", "DROPPED");
        seedBoundKnowledgeBase("kb_course_sql", "student_dropped", "course_sql", Visibility.PUBLIC);
        seedIndexedChunk("kb_course_sql", "doc_course_sql", "Course-bound SQL JOIN material.");

        assertThatThrownBy(() -> ragQueryService.query(
                "student_dropped",
                false,
                false,
                List.of("kb_course_sql"),
                "What does the course say about SQL JOIN?",
                5
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThat(queryLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();
    }

    @Test
    void legacyLiteralAdminCannotReadForeignPrivateKnowledgeBaseOrPersistArtifacts() {
        seedPrivateKnowledgeBase("kb_foreign", "bob");
        seedIndexedChunk("kb_foreign", "doc_foreign", "Hidden admin-only material.");

        assertThatThrownBy(() -> ragQueryService.query(
                "admin",
                List.of("kb_foreign"),
                "What is hidden?",
                5
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThat(queryLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();
    }

    @Test
    void roleAwareAdminRequestIdReplayCanReadForeignPrivateKnowledgeBaseWithoutDuplicateArtifacts() {
        seedPrivateKnowledgeBase("kb_foreign", "bob");
        seedIndexedChunk("kb_foreign", "doc_foreign", "Hidden admin-only material.");

        var first = ragQueryService.queryWithTraceIdAndRequestId(
                "ops_admin",
                true,
                false,
                List.of("kb_foreign"),
                "What is hidden?",
                5,
                "trc_admin_first",
                "req_admin_foreign"
        );
        var second = ragQueryService.queryWithTraceIdAndRequestId(
                "ops_admin",
                true,
                false,
                List.of("kb_foreign"),
                "What is hidden?",
                5,
                "trc_admin_second",
                "req_admin_foreign"
        );

        assertThat(second.traceId()).isEqualTo(first.traceId());
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
    }

    @Test
    void replaysExistingResponseWithSameRequestIdWithoutDuplicatingQueryArtifacts() {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        double beforeReplayCount = ragQueryCount("replay", "true");
        var first = ragQueryService.queryWithTraceIdAndRequestId(
                "alice",
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "trc_rag_replay_first",
                "req_rag_replay"
        );
        var second = ragQueryService.queryWithTraceIdAndRequestId(
                "alice",
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "trc_rag_replay_second",
                "req_rag_replay"
        );

        assertThat(second.traceId()).isEqualTo(first.traceId());
        assertThat(second.answer()).isEqualTo(first.answer());
        assertThat(second.sources()).hasSize(1);
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
        assertThat(ragQueryCount("replay", "true")).isEqualTo(beforeReplayCount + 1.0);
        assertThat(meterRegistry.find("learningos.rag.query.duration")
                .tag("replayed", "true")
                .timer()).isNull();
    }

    @Test
    void rejectsSameRequestIdWhenPayloadHashDiffers() {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        ragQueryService.queryWithTraceIdAndRequestId(
                "alice",
                List.of("kb_sql"),
                "Why does SQL JOIN duplicate rows?",
                5,
                "trc_rag_conflict_first",
                "req_rag_conflict"
        );

        assertThatThrownBy(() -> ragQueryService.queryWithTraceIdAndRequestId(
                "alice",
                List.of("kb_sql"),
                "How should I diagnose SQL JOIN mistakes?",
                5,
                "trc_rag_conflict_second",
                "req_rag_conflict"
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
    }

    @Test
    void replaysNoSourceResponseWithoutPersistingCitations() {
        seedPrivateKnowledgeBase("kb_empty", "alice");

        var first = ragQueryService.queryWithTraceIdAndRequestId(
                "alice",
                List.of("kb_empty"),
                "What does this course say about graphs?",
                5,
                "trc_rag_no_source_first",
                "req_rag_no_source_replay"
        );
        var second = ragQueryService.queryWithTraceIdAndRequestId(
                "alice",
                List.of("kb_empty"),
                "What does this course say about graphs?",
                5,
                "trc_rag_no_source_second",
                "req_rag_no_source_replay"
        );

        assertThat(first.retrieval().noSource()).isTrue();
        assertThat(second.traceId()).isEqualTo("trc_rag_no_source_first");
        assertThat(second.retrieval().noSource()).isTrue();
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isZero();
    }

    @Test
    void returnsExplicitNoSourceMetadataAndDoesNotPersistCitationsWhenAllowedKbHasNoChunks() {
        seedPrivateKnowledgeBase("kb_empty", "alice");
        double beforeNoSourceCount = ragQueryCount("no_source", "false");

        var response = ragQueryService.query("alice", List.of("kb_empty"), "What does this course say about graphs?", 5);

        assertThat(response.answer()).contains("No cited course material was found");
        assertThat(response.sources()).isEmpty();
        assertThat(response.retrieval()).isNotNull();
        assertThat(response.retrieval().strategy()).isEqualTo("NO_SOURCE_REFUSAL");
        assertThat(response.retrieval().queryComplexity()).isEqualTo("SIMPLE");
        assertThat(response.retrieval().noSource()).isTrue();
        assertThat(response.retrieval().retrievalCount()).isZero();
        assertThat(response.retrieval().candidateCount()).isZero();
        assertThat(response.retrieval().citationCount()).isZero();
        assertThat(response.retrieval().downgraded()).isTrue();
        assertThat(response.retrieval().message()).contains("No indexed course chunks");
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isZero();
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("retrievalCount")).isEqualTo(0);
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("SKIPPED_NO_CANDIDATES");
        assertThat((String) logFields.getPropertyValue("sourcesJson"))
                .contains("\"strategy\":\"NO_SOURCE_REFUSAL\"")
                .contains("\"status\":\"SKIPPED_NO_CANDIDATES\"")
                .contains("\"candidateCount\":0")
                .contains("\"citationCount\":0")
                .contains("\"downgraded\":true")
                .contains("\"noSource\":true");
        assertThat(meterRegistry.find("learningos.rag.query.count")
                .tag("strategy", "NO_SOURCE_REFUSAL")
                .tag("outcome", "no_source")
                .tag("no_source", "true")
                .tag("error_code", "OK")
                .counter()
                .count()).isGreaterThanOrEqualTo(beforeNoSourceCount + 1.0);
    }

    @Test
    void routesDiagnosticQuestionsWithLearnerProfileCluesToProfileRag() {
        seedIndexedChunk("kb_sql", "doc_sql", "JOIN mistakes often indicate weak cardinality understanding.");

        var response = ragQueryService.query(
                "alice",
                List.of("kb_sql"),
                "Diagnose my weak points for SQL joins using my learner profile",
                5
        );

        assertThat(response.sources()).hasSize(1);
        assertThat(response.retrieval().strategy()).isEqualTo("RAG_WITH_PROFILE");
        assertThat(response.retrieval().queryComplexity()).isEqualTo("DIAGNOSTIC");
        assertThat(response.retrieval().candidateCount()).isEqualTo(1);
        assertThat(response.retrieval().citationCount()).isEqualTo(1);
        assertThat(response.retrieval().downgraded()).isFalse();
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("NOT_CONFIGURED");
        assertThat((String) logFields.getPropertyValue("sourcesJson"))
                .contains("\"strategy\":\"RAG_WITH_PROFILE\"")
                .contains("\"status\":\"NOT_CONFIGURED\"");
    }

    @Test
    void routesQuestionsWithRecentAnswerHistoryCluesToHistoryRag() {
        seedIndexedChunk("kb_sql", "doc_sql", "Past JOIN errors should be checked against relationship cardinality.");

        var response = ragQueryService.query(
                "alice",
                List.of("kb_sql"),
                "Use my recent answer history to explain why I keep missing SQL join questions",
                5
        );

        assertThat(response.sources()).hasSize(1);
        assertThat(response.retrieval().strategy()).isEqualTo("RAG_WITH_HISTORY");
        assertThat(response.retrieval().queryComplexity()).isEqualTo("DIAGNOSTIC");
        assertThat(response.retrieval().candidateCount()).isEqualTo(1);
        assertThat(response.retrieval().citationCount()).isEqualTo(1);
        assertThat(response.retrieval().downgraded()).isFalse();
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("NOT_CONFIGURED");
        assertThat((String) logFields.getPropertyValue("sourcesJson"))
                .contains("\"strategy\":\"RAG_WITH_HISTORY\"")
                .contains("\"status\":\"NOT_CONFIGURED\"");
    }

    @Test
    void refusesQueryWhenRequestedKnowledgeBaseIsNotAllowed() {
        seedPrivateKnowledgeBase("kb_hidden", "bob");
        double beforeFailureCount = ragFailureCount("FORBIDDEN");

        assertThatThrownBy(() -> ragQueryService.query("alice", List.of("kb_hidden"), "secret?", 5))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No accessible knowledge bases");
        assertThat(queryLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();
        assertThat(ragFailureCount("FORBIDDEN")).isEqualTo(beforeFailureCount + 1.0);
        assertNoSensitiveMetricTags();
    }

    private long ragDurationCount(String strategy, String outcome, String noSource, String replayed, String errorCode) {
        var timer = meterRegistry.find("learningos.rag.query.duration")
                .tag("strategy", strategy)
                .tag("outcome", outcome)
                .tag("no_source", noSource)
                .tag("replayed", replayed)
                .tag("error_code", errorCode)
                .timer();
        return timer == null ? 0 : timer.count();
    }

    private double ragQueryCount(String outcome, String replayed) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> "learningos.rag.query.count".equals(meter.getId().getName()))
                .filter(meter -> outcome.equals(meter.getId().getTag("outcome")))
                .filter(meter -> replayed.equals(meter.getId().getTag("replayed")))
                .filter(Counter.class::isInstance)
                .map(Counter.class::cast)
                .mapToDouble(Counter::count)
                .sum();
    }

    private double ragFailureCount(String errorCode) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> "learningos.rag.query.failures".equals(meter.getId().getName()))
                .filter(meter -> "error".equals(meter.getId().getTag("outcome")))
                .filter(meter -> errorCode.equals(meter.getId().getTag("error_code")))
                .filter(Counter.class::isInstance)
                .map(Counter.class::cast)
                .mapToDouble(Counter::count)
                .sum();
    }

    @Test
    void rejectsMixedAllowedAndForbiddenKnowledgeBasesWithoutPersistingArtifacts() {
        seedIndexedChunk("kb_allowed", "doc_allowed", "SQL JOIN duplicates usually come from one-to-many relationships.");
        seedPrivateKnowledgeBase("kb_hidden", "bob");

        assertThatThrownBy(() -> ragQueryService.query(
                "alice",
                List.of("kb_allowed", "kb_hidden"),
                "Why does SQL JOIN duplicate rows?",
                5
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThat(queryLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();
    }

    @Test
    void blocksUnsafeAcademicQuestionBeforeRetrieval() {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        assertThatThrownBy(() -> ragQueryService.query("alice", List.of("kb_sql"), "Give me exam cheating instructions", 5))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Content safety");
    }

    private void seedIndexedChunk(String kbId, String documentId, String content) {
        seedIndexedChunk(kbId, documentId, content, "chunk_1", Instant.now(), "Multi table joins");
    }

    private void seedIndexedChunk(
            String kbId,
            String documentId,
            String content,
            String chunkId,
            Instant createdAt,
            String sectionTitle
    ) {
        KnowledgeBase kb = seedPrivateKnowledgeBaseIfAbsent(kbId, "alice");
        KbDocument document = new KbDocument();
        document.setId(documentId);
        document.setKnowledgeBase(kb);
        document.setKbId(kbId);
        document.setCourseId(kb.getCourseId());
        document.setName("database-course.md");
        document.setContentType("text/markdown");
        document.setSizeBytes((long) content.length());
        document.setStorageBucket("test");
        document.setStorageKey("test/" + documentId);
        document.setVersion(1);
        document.setParseStatus(DocumentStatus.INDEXED);
        document.setIndexStatus(DocumentStatus.INDEXED);
        document.setCreatedBy("alice");
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);

        KbDocChunk chunk = new KbDocChunk();
        chunk.setId(chunkId);
        chunk.setKbId(kbId);
        chunk.setDocument(document);
        chunk.setDocumentId(documentId);
        chunk.setDocumentVersion(1);
        chunk.setChunkIndex(0);
        chunk.setContent(content);
        chunk.setPageNum(12);
        chunk.setSectionTitle(sectionTitle);
        chunk.setMetadataJson("{}");
        chunk.setCreatedAt(createdAt);
        chunkRepository.save(chunk);
    }

    private KnowledgeBase seedPrivateKnowledgeBaseIfAbsent(String id, String owner) {
        return knowledgeBaseRepository.findById(id)
                .orElseGet(() -> seedPrivateKnowledgeBase(id, owner));
    }

    private KnowledgeBase seedPrivateKnowledgeBase(String id, String owner) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setName(id);
        kb.setDescription(id + " description");
        kb.setVisibility(Visibility.PRIVATE);
        kb.setOwnerUserId(owner);
        kb.setCreatedBy(owner);
        kb.setCreatedAt(Instant.now());
        kb.setUpdatedAt(Instant.now());
        return knowledgeBaseRepository.save(kb);
    }

    private KnowledgeBase seedBoundKnowledgeBase(String id, String owner, String courseId, Visibility visibility) {
        KnowledgeBase kb = seedPrivateKnowledgeBase(id, owner);
        kb.setVisibility(visibility);
        kb.setCourseId(courseId);
        kb.setBindingStatus(KnowledgeBaseBindingStatus.BOUND);
        kb.setBoundBy(owner);
        kb.setBoundAt(Instant.now());
        return knowledgeBaseRepository.saveAndFlush(kb);
    }

    private void seedCourse(String courseId, String teacherId) {
        Course course = new Course();
        course.setId(courseId);
        course.setTitle(courseId);
        course.setDescription(courseId + " description");
        course.setTeacherId(teacherId);
        courseRepository.saveAndFlush(course);
    }

    private void seedEnrollment(String courseId, String learnerId, String status) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setLearnerId(learnerId);
        enrollment.setStatus(status);
        courseEnrollmentRepository.saveAndFlush(enrollment);
    }

    private void assertNoSensitiveMetricTags() {
        assertThat(meterRegistry.getMeters().stream()
                .map(Meter::getId)
                .flatMap(id -> id.getTags().stream())
                .map(tag -> tag.getKey())
                .toList())
                .doesNotContain("traceId", "userId", "requestId", "agentTaskId", "kbId", "documentId",
                        "question", "prompt", "source", "errorMessage");
    }

    @TestConfiguration
    static class MetricsTestConfig {

        @Bean
        SimpleMeterRegistry simpleMeterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        @Primary
        com.learningos.common.observability.LearningOsMetrics learningOsMetrics(MeterRegistry meterRegistry) {
            return new com.learningos.common.observability.LearningOsMetrics(meterRegistry);
        }

        @Bean
        @Primary
        TestRerankerService testRerankerService(com.learningos.config.RagProperties ragProperties) {
            return new TestRerankerService(ragProperties);
        }
    }

    static class TestRerankerService extends RerankerService {

        private Mode mode = Mode.NOT_CONFIGURED;

        TestRerankerService(com.learningos.config.RagProperties ragProperties) {
            super(ragProperties);
        }

        void reset() {
            mode = Mode.NOT_CONFIGURED;
        }

        void timeout() {
            mode = Mode.TIMEOUT;
        }

        void failWithRawMessage() {
            mode = Mode.ERROR;
        }

        @Override
        protected boolean hasConfiguredReranker() {
            return mode != Mode.NOT_CONFIGURED;
        }

        @Override
        protected List<KbDocChunk> invokeReranker(String question, List<KbDocChunk> chunks, int topK) {
            if (mode == Mode.TIMEOUT) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return chunks;
            }
            if (mode == Mode.ERROR) {
                throw new IllegalStateException("provider said apiKey=sk-test raw chunk very sensitive");
            }
            return chunks;
        }

        private enum Mode {
            NOT_CONFIGURED,
            TIMEOUT,
            ERROR
        }
    }
}
