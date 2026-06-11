package com.learningos.assessment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.assessment.domain.AnswerRecord;
import com.learningos.assessment.dto.AnswerSubmitRequest;
import com.learningos.assessment.dto.AnswerSubmitResponse;
import com.learningos.assessment.dto.ReplanDecisionResponse;
import com.learningos.assessment.repository.AnswerRecordRepository;
import com.learningos.assessment.repository.GradingResultRepository;
import com.learningos.assessment.repository.WrongQuestionRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
import com.learningos.learning.application.LearningPathReplanService;
import com.learningos.safety.application.ContentSafetyService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssessmentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ContentSafetyService contentSafetyService = new ContentSafetyService();
    private final AnswerRecordRepository answerRecordRepository = mock(AnswerRecordRepository.class);
    private final GradingResultRepository gradingResultRepository = mock(GradingResultRepository.class);
    private final MasteryRecordRepository masteryRecordRepository = mock(MasteryRecordRepository.class);
    private final WrongQuestionRepository wrongQuestionRepository = mock(WrongQuestionRepository.class);
    private final LearningEventRepository learningEventRepository = mock(LearningEventRepository.class);
    private final CourseAccessService courseAccessService = mock(CourseAccessService.class);
    private final KnowledgePointRepository knowledgePointRepository = mock(KnowledgePointRepository.class);
    private final AssessmentFeedbackService assessmentFeedbackService = new AssessmentFeedbackService();
    private final LearningPathReplanService learningPathReplanService = mock(LearningPathReplanService.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    @Test
    void doesNotExposeLegacySubjectNameRoleReadOverloads() {
        assertNoDeclaredMethod(
                "listAnswers",
                String.class,
                String.class,
                String.class,
                int.class,
                int.class
        );
        assertNoDeclaredMethod(
                "listWrongQuestions",
                String.class,
                String.class,
                String.class,
                int.class,
                int.class
        );
        assertNoDeclaredMethod("answerDetail", String.class, String.class);
        assertNoDeclaredMethod("wrongQuestionDetail", String.class, String.class);
    }

    @Test
    void doesNotKeepSubjectNameInferenceHelpers() {
        assertNoDeclaredMethod("isAdmin", String.class);
        assertNoDeclaredMethod("isTeacherUser", String.class);
    }

    @Test
    void submitAnswerWithTraceIdReplaysExistingResponseWhenRequestIdAlreadyCompleted() throws Exception {
        AnswerSubmitRequest request = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "req_existing_once"
        );
        AnswerSubmitResponse storedResponse = new AnswerSubmitResponse(
                "ans_existing",
                "grd_existing",
                0.85,
                List.of(),
                "fb_existing",
                false,
                "trc_existing",
                new ReplanDecisionResponse("NO_REPLAN_REQUIRED", false, List.of(), "Already handled.", "trc_existing"),
                "Already handled.",
                null,
                "PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB",
                "trc_existing"
        );
        AnswerRecord existing = existingAnswer(request, storedResponse);
        when(answerRecordRepository.findByLearnerIdAndRequestId("alice", "req_existing_once"))
                .thenReturn(Optional.of(existing));

        AnswerSubmitResponse response = service().submitAnswerWithTraceId("alice", request, "trc_ignored_on_replay");

        assertThat(response.answerId()).isEqualTo("ans_existing");
        assertThat(response.traceId()).isEqualTo("trc_existing");
        verify(answerRecordRepository, never()).saveAndFlush(any(AnswerRecord.class));
    }

    @Test
    void replayAnswerIfPresentReturnsEmptyWhenRequestIdWasNotUsed() {
        AnswerSubmitRequest request = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "req_new_once"
        );
        when(answerRecordRepository.findByLearnerIdAndRequestId("alice", "req_new_once"))
                .thenReturn(Optional.empty());

        assertThat(service().replayAnswerIfPresent("alice", request)).isEmpty();
    }

    @Test
    void submitAnswerWithTraceIdRejectsBlankRequestIdAtServiceLayer() {
        AnswerSubmitRequest request = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "   "
        );

        assertThatThrownBy(() -> service().submitAnswerWithTraceId("alice", request, "trc_blank_request"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(answerRecordRepository, never()).findByLearnerIdAndRequestId(any(), any());
    }

    @Test
    void submitAnswerWithTraceIdRejectsTooLongRequestIdAtServiceLayer() {
        AnswerSubmitRequest request = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "r".repeat(121)
        );

        assertThatThrownBy(() -> service().submitAnswerWithTraceId("alice", request, "trc_long_request"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(answerRecordRepository, never()).findByLearnerIdAndRequestId(any(), any());
    }

    @Test
    void replayAnswerIfPresentRejectsBlankRequestIdAtServiceLayer() {
        AnswerSubmitRequest request = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                ""
        );

        assertThatThrownBy(() -> service().replayAnswerIfPresent("alice", request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(answerRecordRepository, never()).findByLearnerIdAndRequestId(any(), any());
    }

    @Test
    void replayAnswerIfPresentRejectsTooLongRequestIdAtServiceLayer() {
        AnswerSubmitRequest request = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "r".repeat(121)
        );

        assertThatThrownBy(() -> service().replayAnswerIfPresent("alice", request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(answerRecordRepository, never()).findByLearnerIdAndRequestId(any(), any());
    }

    @Test
    void replayAnswerIfPresentRejectsExistingRequestIdWhenPayloadHashDiffers() throws Exception {
        AnswerSubmitRequest original = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "req_conflict_once"
        );
        AnswerSubmitRequest conflicting = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "A different answer tries to reuse the same request id.",
                "req_conflict_once"
        );
        AnswerSubmitResponse storedResponse = new AnswerSubmitResponse(
                "ans_conflict",
                "grd_conflict",
                0.85,
                List.of(),
                "fb_conflict",
                false,
                "trc_conflict",
                new ReplanDecisionResponse("NO_REPLAN_REQUIRED", false, List.of(), "Already handled.", "trc_conflict"),
                "Already handled.",
                null,
                "PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB",
                "trc_conflict"
        );
        when(answerRecordRepository.findByLearnerIdAndRequestId("alice", "req_conflict_once"))
                .thenReturn(Optional.of(existingAnswer(original, storedResponse)));

        assertThatThrownBy(() -> service().replayAnswerIfPresent("alice", conflicting))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void replayAnswerIfPresentRejectsInProgressRequestIdWhenResponseSnapshotMissing() throws Exception {
        AnswerSubmitRequest request = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "req_in_progress_once"
        );
        AnswerRecord existing = existingAnswer(request, null);
        existing.setResponseJson("");
        when(answerRecordRepository.findByLearnerIdAndRequestId("alice", "req_in_progress_once"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().replayAnswerIfPresent("alice", request))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception.getMessage()).contains("already being processed");
                });
    }

    @Test
    void replaysExistingResponseWhenConcurrentInsertHitsIdempotencyConstraint() throws Exception {
        AnswerSubmitRequest request = new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "req_race_once"
        );
        AnswerSubmitResponse storedResponse = new AnswerSubmitResponse(
                "ans_winner",
                "grd_winner",
                0.85,
                List.of(),
                "fb_winner",
                false,
                "trc_winner",
                new ReplanDecisionResponse("NO_REPLAN_REQUIRED", false, List.of(), "Already handled.", "trc_winner"),
                "Already handled.",
                null,
                "PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB",
                "trc_winner"
        );
        AnswerRecord winner = new AnswerRecord();
        winner.setId("ans_winner");
        winner.setLearnerId("alice");
        winner.setQuestionId("q_sql_join");
        winner.setAnswer(request.answer());
        winner.setRequestId("req_race_once");
        winner.setRequestHash(payloadHash(request));
        winner.setResponseJson(objectMapper.writeValueAsString(storedResponse));

        when(answerRecordRepository.findByLearnerIdAndRequestId("alice", "req_race_once"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(answerRecordRepository.save(any(AnswerRecord.class)))
                .thenThrow(new DataIntegrityViolationException("uk_answer_learner_request"));
        when(answerRecordRepository.saveAndFlush(any(AnswerRecord.class)))
                .thenThrow(new DataIntegrityViolationException("uk_answer_learner_request"));
        when(masteryRecordRepository.findFirstByLearnerIdAndKnowledgePointIdOrderByUpdatedAtDesc("alice", "kp_sql_join"))
                .thenReturn(Optional.empty());
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());

        AssessmentService service = service();

        AnswerSubmitResponse response = service.submitAnswer("alice", request);

        assertThat(response.answerId()).isEqualTo("ans_winner");
        assertThat(response.traceId()).isEqualTo("trc_winner");
        verify(answerRecordRepository, times(2)).findByLearnerIdAndRequestId("alice", "req_race_once");
    }

    private AssessmentService service() {
        return new AssessmentService(
                contentSafetyService,
                answerRecordRepository,
                gradingResultRepository,
                masteryRecordRepository,
                wrongQuestionRepository,
                learningEventRepository,
                courseAccessService,
                knowledgePointRepository,
                objectMapper,
                assessmentFeedbackService,
                learningPathReplanService,
                transactionManager
        );
    }

    private void assertNoDeclaredMethod(String methodName, Class<?>... parameterTypes) {
        assertThat(Arrays.stream(AssessmentService.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> Arrays.equals(method.getParameterTypes(), parameterTypes))
                .toList())
                .as("AssessmentService should not expose %s%s", methodName, Arrays.toString(parameterTypes))
                .isEmpty();
    }

    private AnswerRecord existingAnswer(AnswerSubmitRequest request, AnswerSubmitResponse response) throws Exception {
        AnswerRecord existing = new AnswerRecord();
        existing.setId(response == null ? "ans_existing" : response.answerId());
        existing.setLearnerId(request.learnerId());
        existing.setQuestionId(request.questionId());
        existing.setAnswer(request.answer());
        existing.setRequestId(request.requestId());
        existing.setRequestHash(payloadHash(request));
        existing.setResponseJson(response == null ? null : objectMapper.writeValueAsString(response));
        return existing;
    }

    private String payloadHash(AnswerSubmitRequest request) throws Exception {
        String payload = String.join("\n", request.learnerId(), request.questionId(), request.answer());
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes()));
    }
}
