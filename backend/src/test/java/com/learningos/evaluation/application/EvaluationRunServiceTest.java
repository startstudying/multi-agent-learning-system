package com.learningos.evaluation.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.evaluation.dto.EvaluationMetricRequest;
import com.learningos.evaluation.dto.EvaluationRunRecordRequest;
import com.learningos.evaluation.dto.EvaluationSampleRequest;
import com.learningos.evaluation.dto.EvaluationSetUpsertRequest;
import com.learningos.evaluation.dto.PromptVersionQualityComparisonResponse;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({EvaluationSetService.class, EvaluationRunService.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class EvaluationRunServiceTest {

    private final EvaluationSetService evaluationSetService;
    private final EvaluationRunService evaluationRunService;
    private final CourseRepository courseRepository;

    EvaluationRunServiceTest(
            EvaluationSetService evaluationSetService,
            EvaluationRunService evaluationRunService,
            CourseRepository courseRepository
    ) {
        this.evaluationSetService = evaluationSetService;
        this.evaluationRunService = evaluationRunService;
        this.courseRepository = courseRepository;
    }

    @Test
    void comparesQualityMetricsAcrossPromptVersionsForSameEvaluationSet() {
        String setId = createRagSet("teacher", null);
        recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("groundedness", 0.55), metric("recallAtK", 0.50)));
        recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v2",
                List.of(metric("groundedness", 0.80), metric("recallAtK", 0.75)));

        var report = compareAsTeacher(
                "teacher",
                setId,
                "rag-answer",
                List.of("agent-rag-v1", "agent-rag-v2"),
                "agent-rag-v1"
        );

        assertThat(report.evaluationSetId()).isEqualTo(setId);
        assertThat(report.promptCode()).isEqualTo("rag-answer");
        assertThat(report.baselinePromptVersion()).isEqualTo("agent-rag-v1");
        assertThat(report.rows()).extracting("promptVersion")
                .containsExactly("agent-rag-v1", "agent-rag-v2");
        assertThat(report.rows().get(0).metrics().get("groundedness").average()).isEqualTo(0.55);
        assertThat(report.rows().get(0).deltas().get("groundedness")).isEqualTo(0.0);
        assertThat(report.rows().get(1).metrics().get("groundedness").average()).isEqualTo(0.80);
        assertThat(report.rows().get(1).deltas().get("groundedness")).isEqualTo(0.25);
        assertThat(report.winnerByMetric()).containsEntry("groundedness", "agent-rag-v2");
    }

    @Test
    void rejectsComparisonWhenLessThanTwoPromptVersionsHaveSucceededRuns() {
        String setId = createRagSet("teacher", null);
        recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("groundedness", 0.55)));

        assertThatThrownBy(() -> compareAsTeacher(
                "teacher",
                setId,
                "rag-answer",
                List.of("agent-rag-v1", "agent-rag-v2"),
                "agent-rag-v1"
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void ignoresFailedRunsWhenCheckingComparablePromptVersions() {
        String setId = createRagSet("teacher", null);
        recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("groundedness", 0.55)));
        evaluationRunService.record("teacher", false, true, runRequest(setId, "rag-answer", "agent-rag-v2", "FAILED",
                List.of(metric("groundedness", 0.95))));

        assertThatThrownBy(() -> compareAsTeacher(
                "teacher",
                setId,
                "rag-answer",
                List.of("agent-rag-v1", "agent-rag-v2"),
                "agent-rag-v1"
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void selectsLowestMeanAbsoluteErrorAsWinner() {
        String setId = createGradingSet();
        recordAsTeacher("teacher", setId, "assessment-grading", "agent-grading-v1",
                List.of(metric("meanAbsoluteError", 0.18), metric("agreementRate", 0.70)));
        recordAsTeacher("teacher", setId, "assessment-grading", "agent-grading-v2",
                List.of(metric("meanAbsoluteError", 0.08), metric("agreementRate", 0.86)));

        var report = compareAsTeacher(
                "teacher",
                setId,
                "assessment-grading",
                List.of("agent-grading-v1", "agent-grading-v2"),
                "agent-grading-v1"
        );

        assertThat(report.winnerByMetric()).containsEntry("meanAbsoluteError", "agent-grading-v2");
        assertThat(report.winnerByMetric()).containsEntry("agreementRate", "agent-grading-v2");
        assertThat(report.rows().get(1).deltas().get("meanAbsoluteError")).isEqualTo(-0.10);
    }

    @Test
    void weightsMetricAveragesByMetricSampleCount() {
        String setId = createRagSet("teacher", null);
        recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("groundedness", 0.90, 1)));
        recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("groundedness", 0.10, 9)));
        recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v2",
                List.of(metric("groundedness", 0.30, 10)));

        var report = compareAsTeacher(
                "teacher",
                setId,
                "rag-answer",
                List.of("agent-rag-v1", "agent-rag-v2"),
                "agent-rag-v1"
        );

        assertThat(report.rows().get(0).metrics().get("groundedness").average()).isEqualTo(0.18);
        assertThat(report.rows().get(1).deltas().get("groundedness")).isEqualTo(0.12);
        assertThat(report.winnerByMetric()).containsEntry("groundedness", "agent-rag-v2");
    }

    @Test
    void teacherCannotCompareForeignCourseEvaluationSet() {
        Course course = new Course();
        course.setId("course_foreign_eval");
        course.setTitle("Foreign evaluation course");
        course.setTeacherId("teacher_b");
        course.setStatus("ACTIVE");
        courseRepository.save(course);

        String setId = createRagSet("admin", "course_foreign_eval");
        recordAsAdmin("admin", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("groundedness", 0.55)));
        recordAsAdmin("admin", setId, "rag-answer", "agent-rag-v2",
                List.of(metric("groundedness", 0.80)));

        assertThatThrownBy(() -> compareAsTeacher(
                "teacher_a",
                setId,
                "rag-answer",
                List.of("agent-rag-v1", "agent-rag-v2"),
                "agent-rag-v1"
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void rejectsRunWhenPromptCodeDoesNotMatchEvaluationSet() {
        String setId = createRagSet("teacher", null);

        assertThatThrownBy(() -> recordAsTeacher("teacher", setId, "other-prompt", "agent-rag-v1",
                List.of(metric("groundedness", 0.55))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void rejectsMetricOutsideWhitelist() {
        String setId = createRagSet("teacher", null);

        assertThatThrownBy(() -> recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("rawOutputScore", 0.99))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void rejectsMetricWithZeroSampleCount() {
        String setId = createRagSet("teacher", null);

        assertThatThrownBy(() -> recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("groundedness", 0.99, 0))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void rejectsSucceededRunWithZeroSampleCount() {
        String setId = createRagSet("teacher", null);

        assertThatThrownBy(() -> evaluationRunService.record("teacher", false, true, runRequest(setId, "rag-answer", "agent-rag-v1",
                "SUCCEEDED", 0, List.of(metric("groundedness", 0.99, 1)))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void allowsFailedRunWithZeroSampleCount() {
        String setId = createRagSet("teacher", null);

        var response = evaluationRunService.record("teacher", false, true, runRequest(setId, "rag-answer", "agent-rag-v1",
                "FAILED", 0, List.of()));

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.sampleCount()).isZero();
        assertThat(response.metrics()).isEmpty();
    }

    @Test
    void rejectsDuplicateMetricNamesInSingleRun() {
        String setId = createRagSet("teacher", null);

        assertThatThrownBy(() -> recordAsTeacher("teacher", setId, "rag-answer", "agent-rag-v1",
                List.of(metric("groundedness", 0.70), metric("groundedness", 0.80))))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    private String createRagSet(String userId, String courseId) {
        boolean admin = "admin".equals(userId);
        return evaluationSetService.upsert(userId, admin, !admin, new EvaluationSetUpsertRequest(
                "rag-quality-comparison-" + userId + "-" + (courseId == null ? "global" : courseId),
                "v1",
                "RAG quality comparison",
                "Prompt version comparison benchmark",
                "RAG_QUESTION",
                "ACTIVE",
                courseId,
                null,
                "rag-answer",
                "agent-rag-v1",
                List.of(new EvaluationSampleRequest(
                        "rag-quality-001",
                        "How should RAG cite course material?",
                        List.of("doc_citation"),
                        3,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("must cite source")
                ))
        )).id();
    }

    private String createGradingSet() {
        return evaluationSetService.upsert("teacher", false, true, new EvaluationSetUpsertRequest(
                "grading-quality-comparison",
                "v1",
                "Grading quality comparison",
                "Prompt version comparison benchmark",
                "GRADING_SAMPLE",
                "ACTIVE",
                null,
                null,
                "assessment-grading",
                "agent-grading-v1",
                List.of(new EvaluationSampleRequest(
                        "grade-quality-001",
                        null,
                        List.of(),
                        null,
                        "q_sql_join",
                        "JOIN duplicates happen when one row matches many rows.",
                        "Score conceptual correctness.",
                        0.85,
                        null,
                        null,
                        null,
                        List.of("score should match human rubric")
                ))
        )).id();
    }

    private void recordAsTeacher(
            String userId,
            String setId,
            String promptCode,
            String promptVersion,
            List<EvaluationMetricRequest> metrics
    ) {
        evaluationRunService.record(userId, false, true, runRequest(setId, promptCode, promptVersion, metrics));
    }

    private void recordAsAdmin(
            String userId,
            String setId,
            String promptCode,
            String promptVersion,
            List<EvaluationMetricRequest> metrics
    ) {
        evaluationRunService.record(userId, true, false, runRequest(setId, promptCode, promptVersion, metrics));
    }

    private PromptVersionQualityComparisonResponse compareAsTeacher(
            String userId,
            String setId,
            String promptCode,
            List<String> promptVersions,
            String baselinePromptVersion
    ) {
        return evaluationRunService.compare(userId, false, true, setId, promptCode, promptVersions, baselinePromptVersion);
    }

    private EvaluationRunRecordRequest runRequest(
            String setId,
            String promptCode,
            String promptVersion,
            List<EvaluationMetricRequest> metrics
    ) {
        return runRequest(setId, promptCode, promptVersion, "SUCCEEDED", metrics);
    }

    private EvaluationRunRecordRequest runRequest(
            String setId,
            String promptCode,
            String promptVersion,
            String status,
            List<EvaluationMetricRequest> metrics
    ) {
        return runRequest(setId, promptCode, promptVersion, status, 10, metrics);
    }

    private EvaluationRunRecordRequest runRequest(
            String setId,
            String promptCode,
            String promptVersion,
            String status,
            int sampleCount,
            List<EvaluationMetricRequest> metrics
    ) {
        return new EvaluationRunRecordRequest(
                setId,
                promptCode,
                promptVersion,
                "mock-model",
                status,
                sampleCount,
                "trace_eval",
                metrics
        );
    }

    private EvaluationMetricRequest metric(String name, double value) {
        return new EvaluationMetricRequest(name, value, "score", 10);
    }

    private EvaluationMetricRequest metric(String name, double value, int sampleCount) {
        return new EvaluationMetricRequest(name, value, "score", sampleCount);
    }
}
