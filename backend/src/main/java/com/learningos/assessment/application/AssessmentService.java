package com.learningos.assessment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.assessment.domain.AnswerRecord;
import com.learningos.assessment.domain.GradingResult;
import com.learningos.assessment.domain.WrongQuestion;
import com.learningos.assessment.dto.AnswerSubmitRequest;
import com.learningos.assessment.dto.AnswerSubmitResponse;
import com.learningos.assessment.dto.AssessmentPageResponse;
import com.learningos.assessment.dto.AssessmentRecordDetailResponse;
import com.learningos.assessment.dto.AssessmentRecordSummaryResponse;
import com.learningos.assessment.dto.MasteryUpdateResponse;
import com.learningos.assessment.dto.ReplanDecisionResponse;
import com.learningos.assessment.dto.WrongQuestionDetailResponse;
import com.learningos.assessment.dto.WrongQuestionSummaryResponse;
import com.learningos.assessment.repository.AnswerRecordRepository;
import com.learningos.assessment.repository.GradingResultRepository;
import com.learningos.assessment.repository.WrongQuestionRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.common.trace.TraceContext;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.domain.KnowledgePoint;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import com.learningos.learning.application.LearningPathReplanDecision;
import com.learningos.learning.application.LearningPathReplanService;
import com.learningos.learning.domain.LearningEvent;
import com.learningos.learning.domain.MasteryRecord;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
import com.learningos.safety.application.ContentSafetyService;
import com.learningos.safety.dto.ContentSafetyResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AssessmentService {

    private static final int MAX_REQUEST_ID_LENGTH = 120;
    private static final int MAX_PAGE_SIZE = 50;

    private final ContentSafetyService contentSafetyService;
    private final AnswerRecordRepository answerRecordRepository;
    private final GradingResultRepository gradingResultRepository;
    private final MasteryRecordRepository masteryRecordRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final LearningEventRepository learningEventRepository;
    private final CourseAccessService courseAccessService;
    private final KnowledgePointRepository knowledgePointRepository;
    private final ObjectMapper objectMapper;
    private final AssessmentFeedbackService assessmentFeedbackService;
    private final LearningPathReplanService learningPathReplanService;
    private final TransactionTemplate transactionTemplate;

    public AssessmentService(
            ContentSafetyService contentSafetyService,
            AnswerRecordRepository answerRecordRepository,
            GradingResultRepository gradingResultRepository,
            MasteryRecordRepository masteryRecordRepository,
            WrongQuestionRepository wrongQuestionRepository,
            LearningEventRepository learningEventRepository,
            CourseAccessService courseAccessService,
            KnowledgePointRepository knowledgePointRepository,
            ObjectMapper objectMapper,
            AssessmentFeedbackService assessmentFeedbackService,
            LearningPathReplanService learningPathReplanService,
            PlatformTransactionManager transactionManager
    ) {
        this.contentSafetyService = contentSafetyService;
        this.answerRecordRepository = answerRecordRepository;
        this.gradingResultRepository = gradingResultRepository;
        this.masteryRecordRepository = masteryRecordRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.learningEventRepository = learningEventRepository;
        this.courseAccessService = courseAccessService;
        this.knowledgePointRepository = knowledgePointRepository;
        this.objectMapper = objectMapper;
        this.assessmentFeedbackService = assessmentFeedbackService;
        this.learningPathReplanService = learningPathReplanService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public AnswerSubmitResponse submitAnswer(String userId, AnswerSubmitRequest request) {
        String traceId = TraceContext.currentTraceId().orElse("trc_" + UUID.randomUUID().toString().replace("-", ""));
        return submitAnswerWithTraceId(userId, request, traceId);
    }

    public AnswerSubmitResponse submitAnswerWithTraceId(String userId, AnswerSubmitRequest request, String traceId) {
        if (!userId.equals(request.learnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner access denied");
        }
        String resolvedTraceId = requireTraceId(traceId);
        String requestId = requireRequestId(request.requestId());
        requireSubmitQuestionScope(request.learnerId(), request.questionId());
        String requestHash = requestHash(request);
        AnswerRecord existingAnswer = answerRecordRepository
                .findByLearnerIdAndRequestId(request.learnerId(), requestId)
                .orElse(null);
        if (existingAnswer != null) {
            return replayExistingAnswer(existingAnswer, requestHash);
        }

        ContentSafetyResult safetyResult = contentSafetyService.checkUserInput(request.answer());
        String answerId = "ans_" + UUID.randomUUID().toString().replace("-", "");
        String gradingResultId = "grd_" + UUID.randomUUID().toString().replace("-", "");
        String feedbackId = "fb_" + UUID.randomUUID().toString().replace("-", "");
        try {
            return transactionTemplate.execute(status -> createAssessmentSubmission(
                    request,
                    safetyResult,
                    resolvedTraceId,
                    answerId,
                    gradingResultId,
                    feedbackId,
                    requestId,
                    requestHash
            ));
        } catch (DataIntegrityViolationException ex) {
            return replayConcurrentSubmission(request.learnerId(), requestId, requestHash, ex);
        }
    }

    public Optional<AnswerSubmitResponse> replayAnswerIfPresent(String userId, AnswerSubmitRequest request) {
        if (!userId.equals(request.learnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner access denied");
        }
        String requestId = requireRequestId(request.requestId());
        requireSubmitQuestionScope(request.learnerId(), request.questionId());
        String requestHash = requestHash(request);
        return answerRecordRepository
                .findByLearnerIdAndRequestId(request.learnerId(), requestId)
                .map(existingAnswer -> replayExistingAnswer(existingAnswer, requestHash));
    }

    @Transactional(readOnly = true)
    public AssessmentPageResponse<AssessmentRecordSummaryResponse> listAnswers(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId,
            int page,
            int size
    ) {
        PageRequest pageable = pageRequest(page, size);
        String normalizedLearnerId = normalizeOptional(learnerId);
        String normalizedCourseId = normalizeOptional(courseId);
        Page<AnswerRecord> records;

        if (currentUserAdmin) {
            records = listAnswersForAdmin(
                    currentUserId,
                    currentUserAdmin,
                    currentUserTeacher,
                    normalizedLearnerId,
                    normalizedCourseId,
                    pageable
            );
        } else if (currentUserTeacher) {
            records = listAnswersForTeacher(
                    currentUserId,
                    currentUserAdmin,
                    currentUserTeacher,
                    normalizedLearnerId,
                    normalizedCourseId,
                    pageable
            );
        } else {
            records = listAnswersForStudent(
                    currentUserId,
                    currentUserAdmin,
                    currentUserTeacher,
                    normalizedLearnerId,
                    normalizedCourseId,
                    pageable
            );
        }

        return AssessmentPageResponse.from(records.map(this::toAnswerSummary));
    }

    @Transactional(readOnly = true)
    public AssessmentPageResponse<WrongQuestionSummaryResponse> listWrongQuestions(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId,
            int page,
            int size
    ) {
        PageRequest pageable = pageRequest(page, size);
        String normalizedLearnerId = normalizeOptional(learnerId);
        String normalizedCourseId = normalizeOptional(courseId);
        Page<WrongQuestion> records;

        if (currentUserAdmin) {
            records = listWrongQuestionsForAdmin(
                    currentUserId,
                    currentUserAdmin,
                    currentUserTeacher,
                    normalizedLearnerId,
                    normalizedCourseId,
                    pageable
            );
        } else if (currentUserTeacher) {
            records = listWrongQuestionsForTeacher(
                    currentUserId,
                    currentUserAdmin,
                    currentUserTeacher,
                    normalizedLearnerId,
                    normalizedCourseId,
                    pageable
            );
        } else {
            records = listWrongQuestionsForStudent(
                    currentUserId,
                    currentUserAdmin,
                    currentUserTeacher,
                    normalizedLearnerId,
                    normalizedCourseId,
                    pageable
            );
        }

        return AssessmentPageResponse.from(records.map(this::toWrongQuestionSummary));
    }

    @Transactional(readOnly = true)
    public AssessmentRecordDetailResponse answerDetail(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String answerId
    ) {
        AnswerRecord answerRecord = requireAnswerReadable(currentUserId, currentUserAdmin, currentUserTeacher, answerId);
        GradingResult gradingResult = gradingResultRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(answerRecord.getId())
                .orElse(null);
        WrongQuestion wrongQuestion = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(answerRecord.getId())
                .orElse(null);
        return new AssessmentRecordDetailResponse(
                answerRecord.getId(),
                answerRecord.getLearnerId(),
                answerRecord.getQuestionId(),
                answerRecord.getAnswer(),
                gradingResult == null ? null : gradingResult.getScore(),
                answerRecord.getSafetyStatus(),
                gradingResult == null ? null : gradingResult.getId(),
                wrongQuestion == null ? null : wrongQuestion.getId(),
                wrongQuestion == null ? null : wrongQuestion.getCauseAnalysis(),
                gradingResult == null ? null : gradingResult.getMasteryUpdatesJson(),
                wrongQuestion == null ? null : wrongQuestion.getResourcePushStrategy(),
                wrongQuestion == null ? null : wrongQuestion.getReplanRecordId(),
                answerRecord.getTraceId()
        );
    }

    @Transactional(readOnly = true)
    public WrongQuestionDetailResponse wrongQuestionDetail(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String wrongQuestionId
    ) {
        WrongQuestion wrongQuestion = requireWrongQuestionReadable(
                currentUserId,
                currentUserAdmin,
                currentUserTeacher,
                wrongQuestionId
        );
        return new WrongQuestionDetailResponse(
                wrongQuestion.getId(),
                wrongQuestion.getAnswerId(),
                wrongQuestion.getGradingResultId(),
                wrongQuestion.getLearnerId(),
                wrongQuestion.getQuestionId(),
                wrongQuestion.getKnowledgePointId(),
                wrongQuestion.getScore(),
                wrongQuestion.getCauseAnalysis(),
                wrongQuestion.getResourcePushStrategy(),
                wrongQuestion.getReplanRecordId(),
                wrongQuestion.getTraceId()
        );
    }

    private Page<AnswerRecord> listAnswersForAdmin(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId,
            PageRequest pageable
    ) {
        if (hasText(courseId)) {
            courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
            List<String> questionIds = questionIdsForCourse(courseId);
            if (questionIds.isEmpty()) {
                return Page.empty(pageable);
            }
            if (hasText(learnerId)) {
                return answerRecordRepository.findByLearnerIdAndQuestionIdIn(learnerId, questionIds, pageable);
            }
            return answerRecordRepository.findByQuestionIdIn(questionIds, pageable);
        }
        if (hasText(learnerId)) {
            return answerRecordRepository.findByLearnerId(learnerId, pageable);
        }
        return answerRecordRepository.findAll(pageable);
    }

    private Page<AnswerRecord> listAnswersForTeacher(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId,
            PageRequest pageable
    ) {
        if (!hasText(courseId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "courseId is required for teacher assessment lists");
        }
        courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
        List<String> activeLearnerIds = scopedTeacherLearnerIds(courseId, learnerId);
        if (activeLearnerIds.isEmpty()) {
            return Page.empty(pageable);
        }
        List<String> questionIds = questionIdsForCourse(courseId);
        if (questionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return answerRecordRepository.findByLearnerIdInAndQuestionIdIn(activeLearnerIds, questionIds, pageable);
    }

    private Page<AnswerRecord> listAnswersForStudent(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId,
            PageRequest pageable
    ) {
        if (hasText(learnerId) && !currentUserId.equals(learnerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Assessment record access denied");
        }
        if (hasText(courseId)) {
            courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
            List<String> questionIds = questionIdsForCourse(courseId);
            if (questionIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return answerRecordRepository.findByLearnerIdAndQuestionIdIn(currentUserId, questionIds, pageable);
        }
        return answerRecordRepository.findByLearnerId(currentUserId, pageable);
    }

    private Page<WrongQuestion> listWrongQuestionsForAdmin(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId,
            PageRequest pageable
    ) {
        if (hasText(courseId)) {
            courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
            List<String> knowledgePointIds = knowledgePointIdsForCourse(courseId);
            if (knowledgePointIds.isEmpty()) {
                return Page.empty(pageable);
            }
            if (hasText(learnerId)) {
                return wrongQuestionRepository.findByLearnerIdAndKnowledgePointIdIn(
                        learnerId,
                        knowledgePointIds,
                        pageable
                );
            }
            return wrongQuestionRepository.findByKnowledgePointIdIn(knowledgePointIds, pageable);
        }
        if (hasText(learnerId)) {
            return wrongQuestionRepository.findByLearnerId(learnerId, pageable);
        }
        return wrongQuestionRepository.findAll(pageable);
    }

    private Page<WrongQuestion> listWrongQuestionsForTeacher(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId,
            PageRequest pageable
    ) {
        if (!hasText(courseId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "courseId is required for teacher assessment lists");
        }
        courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
        List<String> activeLearnerIds = scopedTeacherLearnerIds(courseId, learnerId);
        if (activeLearnerIds.isEmpty()) {
            return Page.empty(pageable);
        }
        List<String> knowledgePointIds = knowledgePointIdsForCourse(courseId);
        if (knowledgePointIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return wrongQuestionRepository.findByLearnerIdInAndKnowledgePointIdIn(
                activeLearnerIds,
                knowledgePointIds,
                pageable
        );
    }

    private Page<WrongQuestion> listWrongQuestionsForStudent(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId,
            PageRequest pageable
    ) {
        if (hasText(learnerId) && !currentUserId.equals(learnerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Assessment record access denied");
        }
        if (hasText(courseId)) {
            courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
            List<String> knowledgePointIds = knowledgePointIdsForCourse(courseId);
            if (knowledgePointIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return wrongQuestionRepository.findByLearnerIdAndKnowledgePointIdIn(
                    currentUserId,
                    knowledgePointIds,
                    pageable
            );
        }
        return wrongQuestionRepository.findByLearnerId(currentUserId, pageable);
    }

    private List<String> scopedTeacherLearnerIds(String courseId, String learnerId) {
        List<String> activeLearnerIds = courseAccessService.listActiveLearnerIds(courseId);
        if (!hasText(learnerId)) {
            return activeLearnerIds;
        }
        if (activeLearnerIds.contains(learnerId)) {
            return List.of(learnerId);
        }
        return List.of();
    }

    private List<String> questionIdsForCourse(String courseId) {
        return knowledgePointIdsForCourse(courseId).stream()
                .map(this::questionIdForKnowledgePointId)
                .toList();
    }

    private List<String> knowledgePointIdsForCourse(String courseId) {
        return knowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId).stream()
                .map(KnowledgePoint::getId)
                .toList();
    }

    private String questionIdForKnowledgePointId(String knowledgePointId) {
        if (knowledgePointId != null && knowledgePointId.startsWith("kp_") && knowledgePointId.length() > 3) {
            return "q_" + knowledgePointId.substring(3);
        }
        return "q_" + knowledgePointId;
    }

    private AssessmentRecordSummaryResponse toAnswerSummary(AnswerRecord answerRecord) {
        GradingResult gradingResult = gradingResultRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(answerRecord.getId())
                .orElse(null);
        WrongQuestion wrongQuestion = wrongQuestionRepository
                .findFirstByAnswerIdOrderByCreatedAtDesc(answerRecord.getId())
                .orElse(null);
        return new AssessmentRecordSummaryResponse(
                answerRecord.getId(),
                answerRecord.getLearnerId(),
                answerRecord.getQuestionId(),
                gradingResult == null ? null : gradingResult.getScore(),
                answerRecord.getSafetyStatus(),
                wrongQuestion == null ? null : wrongQuestion.getId(),
                wrongQuestion == null ? null : wrongQuestion.getResourcePushStrategy(),
                answerRecord.getTraceId(),
                answerRecord.getCreatedAt()
        );
    }

    private WrongQuestionSummaryResponse toWrongQuestionSummary(WrongQuestion wrongQuestion) {
        return new WrongQuestionSummaryResponse(
                wrongQuestion.getId(),
                wrongQuestion.getAnswerId(),
                wrongQuestion.getLearnerId(),
                wrongQuestion.getQuestionId(),
                wrongQuestion.getKnowledgePointId(),
                wrongQuestion.getScore(),
                wrongQuestion.getResourcePushStrategy(),
                wrongQuestion.getTraceId(),
                wrongQuestion.getCreatedAt()
        );
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "size must be between 1 and 50");
        }
        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AnswerRecord requireAnswerReadable(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String answerId
    ) {
        AnswerRecord answerRecord = answerRecordRepository.findById(answerId)
                .orElseThrow(() -> scopedAssessmentMissing(currentUserAdmin));
        if (currentUserAdmin || currentUserId.equals(answerRecord.getLearnerId())) {
            return answerRecord;
        }
        if (currentUserTeacher
                && canTeacherReadQuestion(
                currentUserId,
                currentUserAdmin,
                currentUserTeacher,
                answerRecord.getLearnerId(),
                answerRecord.getQuestionId()
        )) {
            return answerRecord;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "Assessment record access denied");
    }

    private WrongQuestion requireWrongQuestionReadable(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String wrongQuestionId
    ) {
        WrongQuestion wrongQuestion = wrongQuestionRepository.findById(wrongQuestionId)
                .orElseThrow(() -> scopedAssessmentMissing(currentUserAdmin));
        if (currentUserAdmin || currentUserId.equals(wrongQuestion.getLearnerId())) {
            return wrongQuestion;
        }
        if (currentUserTeacher
                && canTeacherReadKnowledgePoint(
                currentUserId,
                currentUserAdmin,
                currentUserTeacher,
                wrongQuestion.getLearnerId(),
                wrongQuestion.getKnowledgePointId()
        )) {
            return wrongQuestion;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "Assessment record access denied");
    }

    private boolean canTeacherReadQuestion(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String questionId
    ) {
        String knowledgePointId = assessmentFeedbackService.resolveKnowledgePointId(questionId);
        return canTeacherReadKnowledgePoint(
                currentUserId,
                currentUserAdmin,
                currentUserTeacher,
                learnerId,
                knowledgePointId
        );
    }

    private boolean canTeacherReadKnowledgePoint(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String knowledgePointId
    ) {
        KnowledgePoint knowledgePoint = knowledgePointRepository.findById(knowledgePointId).orElse(null);
        if (knowledgePoint == null || !hasText(knowledgePoint.getCourseId())) {
            return false;
        }
        try {
            courseAccessService.requireCourseRead(
                    currentUserId,
                    currentUserAdmin,
                    currentUserTeacher,
                    knowledgePoint.getCourseId()
            );
        } catch (ApiException ex) {
            return false;
        }
        return courseAccessService.listActiveLearnerIds(knowledgePoint.getCourseId()).contains(learnerId);
    }

    private ApiException scopedAssessmentMissing(boolean currentUserAdmin) {
        if (currentUserAdmin) {
            return new ApiException(ErrorCode.NOT_FOUND, "Assessment record not found");
        }
        return new ApiException(ErrorCode.FORBIDDEN, "Assessment record access denied");
    }

    private void requireSubmitQuestionScope(String learnerId, String questionId) {
        String knowledgePointId = assessmentFeedbackService.resolveKnowledgePointId(questionId);
        knowledgePointRepository.findById(knowledgePointId)
                .map(KnowledgePoint::getCourseId)
                .filter(this::hasText)
                .ifPresent(courseId -> courseAccessService.requireCourseRead(
                        learnerId,
                        false,
                        false,
                        courseId
                ));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    protected AnswerSubmitResponse createAssessmentSubmission(
            AnswerSubmitRequest request,
            ContentSafetyResult safetyResult,
            String traceId,
            String answerId,
            String gradingResultId,
            String feedbackId,
            String requestId,
            String requestHash
    ) {
        AnswerRecord answerRecord = new AnswerRecord();
        answerRecord.setId(answerId);
        answerRecord.setLearnerId(request.learnerId());
        answerRecord.setQuestionId(request.questionId());
        answerRecord.setAnswer(request.answer());
        answerRecord.setRequestId(requestId);
        answerRecord.setRequestHash(requestHash);
        answerRecord.setSafetyStatus(safetyResult.status().name());
        answerRecord.setTraceId(traceId);
        AnswerRecord persistedAnswerRecord = answerRecordRepository.saveAndFlush(answerRecord);

        String knowledgePointId = assessmentFeedbackService.resolveKnowledgePointId(request.questionId());
        double beforeMastery = masteryRecordRepository
                .findFirstByLearnerIdAndKnowledgePointIdOrderByUpdatedAtDesc(request.learnerId(), knowledgePointId)
                .map(MasteryRecord::getMastery)
                .orElse(assessmentFeedbackService.defaultInitialMastery());
        AssessmentFeedbackEvaluation evaluation = assessmentFeedbackService.evaluate(request, beforeMastery);
        String replanRecordId = traceId;

        ReplanDecisionResponse replanDecision = persistAssessmentResult(
                request,
                traceId,
                safetyResult,
                answerId,
                gradingResultId,
                evaluation,
                replanRecordId,
                persistedAnswerRecord
        );

        AnswerSubmitResponse response = new AnswerSubmitResponse(
                answerId,
                gradingResultId,
                evaluation.score(),
                evaluation.masteryUpdates(),
                feedbackId,
                replanDecision.replanRequired(),
                replanRecordId,
                replanDecision,
                evaluation.wrongCauseAnalysis(),
                evaluation.feedbackDiagnosis(),
                evaluation.resourcePushStrategy(),
                traceId
        );
        persistedAnswerRecord.setResponseJson(toJson(response));
        answerRecordRepository.save(persistedAnswerRecord);
        return response;
    }

    private ReplanDecisionResponse persistAssessmentResult(
            AnswerSubmitRequest request,
            String traceId,
            ContentSafetyResult safetyResult,
            String answerId,
            String gradingResultId,
            AssessmentFeedbackEvaluation evaluation,
            String replanRecordId,
            AnswerRecord answerRecord
    ) {
        GradingResult gradingResult = new GradingResult();
        gradingResult.setId(gradingResultId);
        gradingResult.setAnswerId(answerId);
        gradingResult.setLearnerId(request.learnerId());
        gradingResult.setQuestionId(request.questionId());
        gradingResult.setScore(evaluation.score());
        gradingResult.setFeedbackSummary(evaluation.wrongCauseAnalysis());
        gradingResult.setMasteryUpdatesJson(toJson(evaluation.masteryUpdates()));
        gradingResult.setTraceId(traceId);
        gradingResultRepository.save(gradingResult);

        List<LearningPathReplanDecision> replanDecisions = new ArrayList<>();
        for (MasteryUpdateResponse update : evaluation.masteryUpdates()) {
            MasteryRecord masteryRecord = new MasteryRecord();
            masteryRecord.setLearnerId(request.learnerId());
            masteryRecord.setKnowledgePointId(update.knowledgePointId());
            masteryRecord.setMastery(update.afterMastery());
            masteryRecord.setSourceType("ASSESSMENT_GRADING");
            masteryRecord.setSourceId(gradingResultId);
            masteryRecord.setReasonSummary(update.reasonSummary());
            masteryRecord.setTraceId(traceId);
            masteryRecordRepository.save(masteryRecord);

            replanDecisions.add(learningPathReplanService.evaluateMasteryUpdate(
                    request.learnerId(),
                    update.knowledgePointId(),
                    update.beforeMastery(),
                    update.afterMastery(),
                    traceId
            ));
        }
        ReplanDecisionResponse replanDecision = toReplanDecisionResponse(replanDecisions, traceId);

        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setLearnerId(request.learnerId());
        wrongQuestion.setQuestionId(request.questionId());
        wrongQuestion.setAnswerId(answerId);
        wrongQuestion.setGradingResultId(gradingResultId);
        wrongQuestion.setKnowledgePointId(evaluation.masteryUpdates().getFirst().knowledgePointId());
        wrongQuestion.setScore(evaluation.score());
        wrongQuestion.setCauseAnalysis(toJson(evaluation.feedbackDiagnosis()));
        wrongQuestion.setResourcePushStrategy(evaluation.resourcePushStrategy());
        wrongQuestion.setReplanRecordId(replanRecordId);
        wrongQuestion.setTraceId(traceId);
        wrongQuestionRepository.save(wrongQuestion);

        LearningEvent event = new LearningEvent();
        event.setLearnerId(request.learnerId());
        event.setEventType("ASSESSMENT_SUBMITTED");
        event.setSubjectId(request.questionId());
        event.setSummary("Submitted answer was graded, mastery was updated, and remediation was planned.");
        event.setPayloadJson(toJson(Map.ofEntries(
                Map.entry("answerId", answerId),
                Map.entry("gradingResultId", gradingResultId),
                Map.entry("questionId", request.questionId()),
                Map.entry("score", evaluation.score()),
                Map.entry("masteryUpdates", evaluation.masteryUpdates()),
                Map.entry("wrongCauseAnalysis", evaluation.wrongCauseAnalysis()),
                Map.entry("feedbackDiagnosis", evaluation.feedbackDiagnosis()),
                Map.entry("resourcePushStrategy", evaluation.resourcePushStrategy()),
                Map.entry("replanRecordId", replanRecordId),
                Map.entry("replanDecision", replanDecision),
                Map.entry("traceId", traceId),
                Map.entry("timestamp", Instant.now().toString())
        )));
        event.setTraceId(traceId);
        learningEventRepository.save(event);

        return replanDecision;
    }

    private ReplanDecisionResponse toReplanDecisionResponse(
            List<LearningPathReplanDecision> decisions,
            String traceId
    ) {
        if (decisions.isEmpty()) {
            return new ReplanDecisionResponse(
                    LearningPathReplanService.STATUS_NO_REPLAN_REQUIRED,
                    false,
                    List.of(),
                    "No mastery updates were produced for this assessment.",
                    traceId
            );
        }

        boolean replanRequired = decisions.stream().anyMatch(LearningPathReplanDecision::replanRequired);
        Set<String> affectedPathIds = new LinkedHashSet<>();
        List<String> reasonSummaries = new ArrayList<>();
        for (LearningPathReplanDecision decision : decisions) {
            affectedPathIds.addAll(decision.affectedPathIds());
            if (decision.reasonSummary() != null && !decision.reasonSummary().isBlank()) {
                reasonSummaries.add(decision.reasonSummary());
            }
        }

        String reasonSummary = String.join(" ", reasonSummaries);
        if (reasonSummary.isBlank()) {
            reasonSummary = replanRequired
                    ? "Assessment mastery update requires learning path replanning."
                    : "Assessment mastery update does not require learning path replanning.";
        }

        return new ReplanDecisionResponse(
                replanRequired
                        ? LearningPathReplanService.STATUS_REPLAN_REQUIRED
                        : LearningPathReplanService.STATUS_NO_REPLAN_REQUIRED,
                replanRequired,
                List.copyOf(affectedPathIds),
                reasonSummary,
                decisions.stream()
                        .map(LearningPathReplanDecision::traceId)
                        .filter(Objects::nonNull)
                        .filter(decisionTraceId -> !decisionTraceId.isBlank())
                        .findFirst()
                        .orElse(traceId)
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize assessment record", ex);
        }
    }

    private AnswerSubmitResponse replayExistingAnswer(AnswerRecord existingAnswer, String requestHash) {
        if (!requestHash.equals(existingAnswer.getRequestHash())) {
            throw new ApiException(ErrorCode.CONFLICT, "requestId already used with different payload");
        }
        if (existingAnswer.getResponseJson() == null || existingAnswer.getResponseJson().isBlank()) {
            throw new ApiException(ErrorCode.CONFLICT, "requestId is already being processed");
        }
        try {
            return objectMapper.readValue(existingAnswer.getResponseJson(), AnswerSubmitResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize assessment response snapshot", ex);
        }
    }

    private AnswerSubmitResponse replayConcurrentSubmission(
            String learnerId,
            String requestId,
            String requestHash,
            DataIntegrityViolationException originalException
    ) {
        return answerRecordRepository.findByLearnerIdAndRequestId(learnerId, requestId)
                .map(existingAnswer -> replayExistingAnswer(existingAnswer, requestHash))
                .orElseThrow(() -> originalException);
    }

    private String requireRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId is required");
        }
        String normalized = requestId.trim();
        if (normalized.length() > MAX_REQUEST_ID_LENGTH) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId length must be less than or equal to 120");
        }
        return normalized;
    }

    private String requireTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "traceId is required");
        }
        return traceId.trim();
    }

    private String requestHash(AnswerSubmitRequest request) {
        String payload = String.join("\n",
                request.learnerId(),
                request.questionId(),
                request.answer()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
