package com.learningos.assessment.application;

import com.learningos.assessment.domain.WrongCauseCategory;
import com.learningos.assessment.dto.AnswerSubmitRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentFeedbackServiceTest {

    private final AssessmentFeedbackService service = new AssessmentFeedbackService();

    @Test
    void diagnosesTransferWeaknessWhenConceptIsNamedButRemediationIsMissing() {
        AssessmentFeedbackEvaluation evaluation = service.evaluate(new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates happen when a one-to-many relation is joined.",
                "req_feedback_transfer"
        ));

        assertThat(evaluation.score()).isEqualTo(0.85);
        assertThat(evaluation.feedbackDiagnosis().wrongCauseCategory())
                .isEqualTo(WrongCauseCategory.TRANSFER_WEAKNESS);
        assertThat(evaluation.feedbackDiagnosis().misconception()).contains("one-to-many");
        assertThat(evaluation.feedbackDiagnosis().recommendedRemediation()).contains("aggregation");
        assertThat(evaluation.masteryUpdates()).singleElement().satisfies(update -> {
            assertThat(update.knowledgePointId()).isEqualTo("kp_sql_join");
            assertThat(update.beforeMastery()).isEqualTo(0.42);
            assertThat(update.afterMastery()).isEqualTo(0.58);
        });
    }

    @Test
    void diagnosesIncompleteExpressionForVeryShortAnswersWithoutInvalidMasteryValues() {
        AssessmentFeedbackEvaluation evaluation = service.evaluate(new AnswerSubmitRequest(
                "alice",
                "q_sql_join",
                "JOIN duplicates.",
                "req_feedback_short"
        ));

        assertThat(evaluation.feedbackDiagnosis().wrongCauseCategory())
                .isEqualTo(WrongCauseCategory.INCOMPLETE_EXPRESSION);
        assertThat(evaluation.score()).isBetween(0.0, 1.0);
        assertThat(evaluation.masteryUpdates())
                .allSatisfy(update -> assertThat(update.afterMastery()).isBetween(0.0, 1.0));
    }
}
