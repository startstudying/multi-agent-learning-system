package com.learningos.assessment.application;

import com.learningos.assessment.dto.GradingEvaluationRequest;
import com.learningos.assessment.dto.GradingEvaluationSampleRequest;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class GradingEvaluationService {

    private static final String UNKNOWN_GROUP = "UNKNOWN";
    private static final String SAMPLE_COURSE_SCOPE_ERROR = "Sample knowledge points must belong to request course";

    private final CourseAccessService courseAccessService;
    private final KnowledgePointRepository knowledgePointRepository;

    public GradingEvaluationService(
            CourseAccessService courseAccessService,
            KnowledgePointRepository knowledgePointRepository
    ) {
        this.courseAccessService = courseAccessService;
        this.knowledgePointRepository = knowledgePointRepository;
    }

    public GradingEvaluationSummary evaluate(GradingEvaluationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Evaluation request is required");
        }
        if (request.samples() != null && !request.samples().isEmpty()) {
            return evaluateSamples(request.samples());
        }
        if (request.hasLegacyScores()) {
            return evaluate(request.humanScores(), request.aiScores(), request.normalizedAgreementThreshold());
        }
        return emptySummary();
    }

    public GradingEvaluationSummary evaluate(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            GradingEvaluationRequest request
    ) {
        if (!currentUserAdmin && !currentUserTeacher) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Grading evaluation access denied");
        }
        if (request == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Evaluation request is required");
        }
        String courseId = normalizeOptional(request.courseId());
        if (!hasText(courseId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "courseId is required");
        }
        courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
        validateSampleKnowledgeScope(courseId, request.samples());
        return evaluate(request);
    }

    public GradingEvaluationSummary evaluate(
            List<Double> humanScores,
            List<Double> aiScores,
            double agreementThreshold
    ) {
        validateInputs(humanScores, aiScores, agreementThreshold);
        if (humanScores.isEmpty()) {
            return emptySummary();
        }

        double absoluteErrorSum = 0.0;
        int agreementCount = 0;
        for (int index = 0; index < humanScores.size(); index++) {
            double absoluteError = Math.abs(humanScores.get(index) - aiScores.get(index));
            absoluteErrorSum += absoluteError;
            if (absoluteError <= agreementThreshold) {
                agreementCount++;
            }
        }

        int sampleCount = humanScores.size();
        double gradeAgreementRate = (double) agreementCount / sampleCount;
        return new GradingEvaluationSummary(absoluteErrorSum / sampleCount, gradeAgreementRate, sampleCount);
    }

    private GradingEvaluationSummary evaluateSamples(List<GradingEvaluationSampleRequest> samples) {
        validateSamples(samples);
        Metrics metrics = calculateMetrics(samples);
        return new GradingEvaluationSummary(
                metrics.meanAbsoluteError(),
                metrics.gradeAgreementRate(),
                metrics.gradeAgreementRate(),
                metrics.wrongCauseAgreementRate(),
                metrics.sampleCount(),
                new GradingEvaluationSummary.GroupedAnalysis(
                        groupedMetrics(samples, GradingEvaluationSampleRequest::questionType),
                        groupedMetrics(samples, GradingEvaluationSampleRequest::knowledgePointId),
                        groupedMetrics(samples, GradingEvaluationSampleRequest::rubricVersion)
                )
        );
    }

    private List<GradingEvaluationSummary.GroupMetrics> groupedMetrics(
            List<GradingEvaluationSampleRequest> samples,
            Function<GradingEvaluationSampleRequest, String> groupKeyExtractor
    ) {
        Map<String, List<GradingEvaluationSampleRequest>> groups = new LinkedHashMap<>();
        for (GradingEvaluationSampleRequest sample : samples) {
            groups.computeIfAbsent(groupKey(groupKeyExtractor.apply(sample)), ignored -> new ArrayList<>()).add(sample);
        }

        List<GradingEvaluationSummary.GroupMetrics> result = new ArrayList<>();
        for (Map.Entry<String, List<GradingEvaluationSampleRequest>> entry : groups.entrySet()) {
            Metrics metrics = calculateMetrics(entry.getValue());
            result.add(new GradingEvaluationSummary.GroupMetrics(
                    entry.getKey(),
                    metrics.sampleCount(),
                    metrics.meanAbsoluteError(),
                    metrics.gradeAgreementRate(),
                    metrics.wrongCauseAgreementRate()
            ));
        }
        return List.copyOf(result);
    }

    private Metrics calculateMetrics(List<GradingEvaluationSampleRequest> samples) {
        if (samples.isEmpty()) {
            return Metrics.empty();
        }

        double absoluteErrorSum = 0.0;
        int gradeComparableCount = 0;
        int gradeAgreementCount = 0;
        int wrongCauseComparableCount = 0;
        int wrongCauseAgreementCount = 0;

        for (GradingEvaluationSampleRequest sample : samples) {
            absoluteErrorSum += Math.abs(sample.humanScore() - sample.systemScore());

            if (isComparable(sample.humanGrade(), sample.systemGrade())) {
                gradeComparableCount++;
                if (normalizedLabel(sample.humanGrade()).equals(normalizedLabel(sample.systemGrade()))) {
                    gradeAgreementCount++;
                }
            }

            if (isComparable(sample.humanWrongCause(), sample.systemWrongCause())) {
                wrongCauseComparableCount++;
                if (normalizedLabel(sample.humanWrongCause()).equals(normalizedLabel(sample.systemWrongCause()))) {
                    wrongCauseAgreementCount++;
                }
            }
        }

        int sampleCount = samples.size();
        return new Metrics(
                sampleCount,
                absoluteErrorSum / sampleCount,
                rate(gradeAgreementCount, gradeComparableCount),
                rate(wrongCauseAgreementCount, wrongCauseComparableCount)
        );
    }

    private void validateInputs(List<Double> humanScores, List<Double> aiScores, double agreementThreshold) {
        if (humanScores == null || aiScores == null) {
            throw new IllegalArgumentException("Score lists are required");
        }
        if (humanScores.size() != aiScores.size()) {
            throw new IllegalArgumentException("Human and AI score lists must have the same size");
        }
        if (agreementThreshold < 0) {
            throw new IllegalArgumentException("Agreement threshold must be non-negative");
        }
        if (humanScores.stream().anyMatch(this::isInvalidScore) || aiScores.stream().anyMatch(this::isInvalidScore)) {
            throw new IllegalArgumentException("Scores must be finite numbers");
        }
    }

    private void validateSamples(List<GradingEvaluationSampleRequest> samples) {
        for (GradingEvaluationSampleRequest sample : samples) {
            if (sample == null) {
                throw new IllegalArgumentException("Samples cannot contain null values");
            }
            if (isInvalidScore(sample.humanScore()) || isInvalidScore(sample.systemScore())) {
                throw new IllegalArgumentException("Sample scores must be finite numbers");
            }
        }
    }

    private void validateSampleKnowledgeScope(String courseId, List<GradingEvaluationSampleRequest> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        Set<String> allowedKnowledgePointIds = knowledgePointIdsForCourse(courseId);
        for (GradingEvaluationSampleRequest sample : samples) {
            if (sample == null) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Samples cannot contain null values");
            }
            String knowledgePointId = normalizeOptional(sample.knowledgePointId());
            if (hasText(knowledgePointId) && !allowedKnowledgePointIds.contains(knowledgePointId)) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, SAMPLE_COURSE_SCOPE_ERROR);
            }
        }
    }

    private Set<String> knowledgePointIdsForCourse(String courseId) {
        Set<String> ids = new HashSet<>();
        knowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId)
                .forEach(point -> ids.add(point.getId()));
        return ids;
    }

    private boolean isInvalidScore(Double score) {
        return score == null || Double.isNaN(score) || Double.isInfinite(score);
    }

    private boolean isComparable(String humanLabel, String systemLabel) {
        return hasText(humanLabel) || hasText(systemLabel);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizedLabel(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String groupKey(String value) {
        return hasText(value) ? value.trim() : UNKNOWN_GROUP;
    }

    private double rate(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private GradingEvaluationSummary emptySummary() {
        return new GradingEvaluationSummary(0.0, 0.0, 0);
    }

    private record Metrics(
            int sampleCount,
            double meanAbsoluteError,
            double gradeAgreementRate,
            double wrongCauseAgreementRate
    ) {
        private static Metrics empty() {
            return new Metrics(0, 0.0, 0.0, 0.0);
        }
    }
}
