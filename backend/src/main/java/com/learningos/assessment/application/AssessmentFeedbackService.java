package com.learningos.assessment.application;

import com.learningos.assessment.domain.WrongCauseCategory;
import com.learningos.assessment.dto.AnswerSubmitRequest;
import com.learningos.assessment.dto.FeedbackDiagnosisResponse;
import com.learningos.assessment.dto.MasteryUpdateResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AssessmentFeedbackService {

    private static final double DEFAULT_INITIAL_MASTERY = 0.42;

    public AssessmentFeedbackEvaluation evaluate(AnswerSubmitRequest request) {
        return evaluate(request, defaultInitialMastery());
    }

    public AssessmentFeedbackEvaluation evaluate(AnswerSubmitRequest request, double beforeMastery) {
        String normalized = normalize(request.answer());
        WrongCauseCategory category = diagnoseCategory(normalized);
        FeedbackDiagnosisResponse diagnosis = diagnosisFor(category, request.answer());

        return new AssessmentFeedbackEvaluation(
                scoreFor(category),
                masteryUpdatesFor(category, request.questionId(), beforeMastery),
                diagnosis,
                summarize(diagnosis),
                resourcePushStrategyFor(category),
                true
        );
    }

    public String resolveKnowledgePointId(String questionId) {
        String normalized = normalizeIdentifier(questionId);
        if (normalized.startsWith("q_") && normalized.length() > 2) {
            return "kp_" + normalized.substring(2);
        }
        if (normalized.startsWith("question_") && normalized.length() > 9) {
            return "kp_" + normalized.substring(9);
        }
        return normalized.isBlank() ? "kp_unknown" : "kp_" + normalized;
    }

    public double defaultInitialMastery() {
        return DEFAULT_INITIAL_MASTERY;
    }

    private WrongCauseCategory diagnoseCategory(String normalizedAnswer) {
        if (wordCount(normalizedAnswer) <= 3) {
            return WrongCauseCategory.INCOMPLETE_EXPRESSION;
        }
        boolean namesJoinDuplication = containsAny(normalizedAnswer, "join", "one-to-many", "one to many")
                && containsAny(normalizedAnswer, "duplicate", "duplication", "duplicated");
        if (!namesJoinDuplication) {
            return WrongCauseCategory.CONCEPT_ERROR;
        }
        boolean namesRemediationStep = containsAny(
                normalizedAnswer,
                "aggregate",
                "aggregation",
                "group by",
                "distinct",
                "predicate",
                "filter"
        );
        if (!namesRemediationStep) {
            return WrongCauseCategory.TRANSFER_WEAKNESS;
        }
        return WrongCauseCategory.STEP_ERROR;
    }

    private double scoreFor(WrongCauseCategory category) {
        return switch (category) {
            case TRANSFER_WEAKNESS -> 0.85;
            case STEP_ERROR -> 0.72;
            case INCOMPLETE_EXPRESSION -> 0.45;
            case CONCEPT_ERROR -> 0.30;
        };
    }

    private List<MasteryUpdateResponse> masteryUpdatesFor(
            WrongCauseCategory category,
            String questionId,
            double beforeMastery
    ) {
        double normalizedBeforeMastery = normalizeMastery(beforeMastery);
        double afterMastery = afterMasteryFor(category, normalizedBeforeMastery);
        return List.of(new MasteryUpdateResponse(
                resolveKnowledgePointId(questionId),
                normalizedBeforeMastery,
                afterMastery,
                reasonSummaryFor(category, normalizedBeforeMastery, afterMastery)
        ));
    }

    private FeedbackDiagnosisResponse diagnosisFor(WrongCauseCategory category, String answer) {
        String evidence = extractEvidence(answer);
        return switch (category) {
            case TRANSFER_WEAKNESS -> new FeedbackDiagnosisResponse(
                    category,
                    "The answer recognizes one-to-many JOIN duplication but does not transfer that concept into a prevention strategy.",
                    "Selecting a remediation pattern such as aggregation, DISTINCT, or stricter join predicates.",
                    "Add a concrete SQL correction step: use aggregation to pre-aggregate child rows, apply GROUP BY, use DISTINCT only when justified, or tighten predicates.",
                    evidence
            );
            case INCOMPLETE_EXPRESSION -> new FeedbackDiagnosisResponse(
                    category,
                    "The answer is too short to show whether the learner understands the mechanism behind JOIN duplication.",
                    "Explaining the table relationship and why rows multiply before proposing a fix.",
                    "Rewrite the answer in two parts: why the duplication happens and which query change prevents it.",
                    evidence
            );
            case CONCEPT_ERROR -> new FeedbackDiagnosisResponse(
                    category,
                    "The answer does not correctly identify one-to-many JOIN row multiplication as the core issue.",
                    "Relationship cardinality and how SQL JOINs combine matching child rows.",
                    "Review one-to-many joins with a small parent-child table example, then explain why parent rows repeat.",
                    evidence
            );
            case STEP_ERROR -> new FeedbackDiagnosisResponse(
                    category,
                    "The answer names relevant JOIN and remediation ideas but leaves the execution order or SQL operation ambiguous.",
                    "Sequencing diagnosis before choosing aggregation, DISTINCT, or predicate tightening.",
                    "State the exact query step you would change and why that step removes the extra rows.",
                    evidence
            );
        };
    }

    private double afterMasteryFor(WrongCauseCategory category, double beforeMastery) {
        return switch (category) {
            case TRANSFER_WEAKNESS -> roundMastery(Math.min(0.78, beforeMastery + 0.16));
            case STEP_ERROR -> roundMastery(Math.min(0.72, beforeMastery + 0.10));
            case INCOMPLETE_EXPRESSION -> roundMastery(Math.min(0.46, beforeMastery + 0.04));
            case CONCEPT_ERROR -> roundMastery(Math.max(0.0, Math.min(0.38, beforeMastery - 0.04)));
        };
    }

    private String reasonSummaryFor(WrongCauseCategory category, double beforeMastery, double afterMastery) {
        String transition = " BKT-lite mastery update: " + beforeMastery + " -> " + afterMastery + ".";
        return switch (category) {
            case TRANSFER_WEAKNESS -> "Answer correctly identified one-to-many JOIN duplication with a remaining transfer gap." + transition;
            case STEP_ERROR -> "Answer showed partial JOIN reasoning but needs clearer remediation steps." + transition;
            case INCOMPLETE_EXPRESSION -> "Answer was too brief to demonstrate complete JOIN reasoning." + transition;
            case CONCEPT_ERROR -> "Answer did not show the expected one-to-many JOIN duplication concept." + transition;
        };
    }

    private String resourcePushStrategyFor(WrongCauseCategory category) {
        return switch (category) {
            case TRANSFER_WEAKNESS, STEP_ERROR -> "PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB";
            case INCOMPLETE_EXPRESSION -> "PUSH_EXPLANATION_TEMPLATE_AND_SHORT_PRACTICE";
            case CONCEPT_ERROR -> "PUSH_CONCEPT_RETEACH_AND_GUIDED_EXAMPLE";
        };
    }

    private String summarize(FeedbackDiagnosisResponse diagnosis) {
        return diagnosis.wrongCauseCategory()
                + ": "
                + diagnosis.misconception()
                + " Recommended remediation: "
                + diagnosis.recommendedRemediation();
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private int wordCount(String text) {
        if (text.isBlank()) {
            return 0;
        }
        return text.split("\\s+").length;
    }

    private String normalize(String answer) {
        return answer == null ? "" : answer.toLowerCase(Locale.ROOT).trim();
    }

    private String normalizeIdentifier(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private double normalizeMastery(double mastery) {
        if (Double.isNaN(mastery) || Double.isInfinite(mastery)) {
            return DEFAULT_INITIAL_MASTERY;
        }
        return roundMastery(Math.max(0.0, Math.min(1.0, mastery)));
    }

    private double roundMastery(double mastery) {
        return Math.round(mastery * 100.0) / 100.0;
    }

    private String extractEvidence(String answer) {
        if (answer == null || answer.isBlank()) {
            return "No learner answer text was provided.";
        }
        String trimmed = answer.trim();
        return trimmed.length() <= 240 ? trimmed : trimmed.substring(0, 240);
    }
}
