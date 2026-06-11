package com.learningos.assessment.application;

import com.learningos.assessment.dto.GradingEvaluationRequest;
import com.learningos.assessment.dto.GradingEvaluationSampleRequest;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

class GradingEvaluationServiceTest {

    private final GradingEvaluationService service = new GradingEvaluationService(
            mock(CourseAccessService.class),
            mock(KnowledgePointRepository.class)
    );

    @Test
    void doesNotExposeLegacySubjectNameRoleEvaluationOverload() {
        assertNoDeclaredMethod("evaluate", String.class, GradingEvaluationRequest.class);
    }

    @Test
    void doesNotKeepSubjectNameInferenceHelpers() {
        assertNoDeclaredMethod("isAdmin", String.class);
        assertNoDeclaredMethod("isTeacherUser", String.class);
    }

    @Test
    void evaluatesHumanScoredSamplesWithAgreementMetricsAndGroupedAnalysis() {
        GradingEvaluationSummary summary = service.evaluate(new GradingEvaluationRequest(List.of(
                sample("sample_1", "SHORT_ANSWER", "kp_sql_join", "rubric-v1", 0.90, 0.80, "A", "B",
                        "TRANSFER_WEAKNESS", "TRANSFER_WEAKNESS"),
                sample("sample_2", "SHORT_ANSWER", "kp_sql_join", "rubric-v1", 0.70, 0.75, "B", "B",
                        "STEP_ERROR", "STEP_ERROR"),
                sample("sample_3", "CHOICE", "kp_sql_filter", "rubric-v2", 0.40, 0.55, "C", "C",
                        "CONCEPT_ERROR", "STEP_ERROR"),
                sample("sample_4", "CHOICE", "kp_sql_filter", "rubric-v2", 0.60, 0.50, "B", "C",
                        "INCOMPLETE_EXPRESSION", "INCOMPLETE_EXPRESSION")
        )));

        assertThat(summary.sampleCount()).isEqualTo(4);
        assertThat(summary.meanAbsoluteError()).isCloseTo(0.10, within(0.0000001));
        assertThat(summary.gradeAgreementRate()).isCloseTo(0.50, within(0.0000001));
        assertThat(summary.agreementRate()).isCloseTo(summary.gradeAgreementRate(), within(0.0000001));
        assertThat(summary.wrongCauseAgreementRate()).isCloseTo(0.75, within(0.0000001));

        assertThat(summary.groupedAnalysis().questionType())
                .extracting(GradingEvaluationSummary.GroupMetrics::groupKey)
                .containsExactly("SHORT_ANSWER", "CHOICE");
        assertThat(summary.groupedAnalysis().questionType().getFirst()).satisfies(group -> {
            assertThat(group.sampleCount()).isEqualTo(2);
            assertThat(group.meanAbsoluteError()).isCloseTo(0.075, within(0.0000001));
            assertThat(group.gradeAgreementRate()).isCloseTo(0.50, within(0.0000001));
            assertThat(group.wrongCauseAgreementRate()).isCloseTo(1.00, within(0.0000001));
        });
        assertThat(summary.groupedAnalysis().knowledgePointId())
                .extracting(GradingEvaluationSummary.GroupMetrics::groupKey)
                .containsExactly("kp_sql_join", "kp_sql_filter");
        assertThat(summary.groupedAnalysis().rubricVersion())
                .extracting(GradingEvaluationSummary.GroupMetrics::groupKey)
                .containsExactly("rubric-v1", "rubric-v2");
    }

    @Test
    void evaluatesMeanAbsoluteErrorAgreementRateAndSampleCount() {
        GradingEvaluationSummary summary = service.evaluate(
                List.of(1.0, 0.7, 0.2),
                List.of(0.96, 0.72, 0.1),
                0.05
        );

        assertThat(summary.sampleCount()).isEqualTo(3);
        assertThat(summary.meanAbsoluteError()).isCloseTo(0.0533333333, within(0.0000001));
        assertThat(summary.agreementRate()).isCloseTo(0.6666666667, within(0.0000001));
    }

    @Test
    void returnsZeroMetricsForEmptySamples() {
        GradingEvaluationSummary summary = service.evaluate(List.of(), List.of(), 0.05);

        assertThat(summary.sampleCount()).isZero();
        assertThat(summary.meanAbsoluteError()).isZero();
        assertThat(summary.agreementRate()).isZero();
    }

    private GradingEvaluationSampleRequest sample(
            String sampleId,
            String questionType,
            String knowledgePointId,
            String rubricVersion,
            double humanScore,
            double systemScore,
            String humanGrade,
            String systemGrade,
            String humanWrongCause,
            String systemWrongCause
    ) {
        return new GradingEvaluationSampleRequest(
                sampleId,
                questionType,
                knowledgePointId,
                rubricVersion,
                humanScore,
                systemScore,
                humanGrade,
                systemGrade,
                humanWrongCause,
                systemWrongCause
        );
    }

    private void assertNoDeclaredMethod(String methodName, Class<?>... parameterTypes) {
        assertThat(Arrays.stream(GradingEvaluationService.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> Arrays.equals(method.getParameterTypes(), parameterTypes))
                .toList())
                .as("GradingEvaluationService should not expose %s%s", methodName, Arrays.toString(parameterTypes))
                .isEmpty();
    }
}
