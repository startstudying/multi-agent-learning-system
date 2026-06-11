package com.learningos.learning.application;

import com.learningos.common.trace.TraceContext;
import com.learningos.learning.dto.CreateLearningPathRequest;
import com.learningos.learning.dto.LearningPathResponse;
import com.learningos.learning.dto.ProfileExtractRequest;
import com.learningos.learning.dto.ProfileExtractResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class LearningWorkflowServiceTest {

    private final LearningWorkflowService service = new LearningWorkflowService();

    @AfterEach
    void clearTraceContext() {
        TraceContext.clear();
    }

    @Test
    void extractsProfileFromDialogueKeywordsWithTraceableEvidence() {
        TraceContext.setCurrentTraceId("trace-profile");

        ProfileExtractResponse response = service.extractProfile(new ProfileExtractRequest(
                "learner_sql",
                "I want Spring Boot APIs, but SQL JOIN duplication and RAG citations confuse me. I like code labs."
        ));

        assertThat(response.traceId()).isEqualTo("trace-profile");
        assertThat(response.profileDraft().target()).isEqualTo("Build production-grade Spring Boot APIs");
        assertThat(response.profileDraft().weakPoints()).containsExactly("SQL JOIN reasoning", "RAG grounding");
        assertThat(response.profileDraft().preferences()).contains("code labs");
        assertThat(response.profileDraft().dimensions()).hasSize(7);
        assertThat(response.profileDraft().dimensions())
                .allSatisfy(dimension -> {
                    assertThat(dimension.evidence()).isNotBlank();
                    assertThat(dimension.lastEvidenceId()).isNotBlank();
                });
        assertThat(response.profileDraft().dimensions())
                .extracting("name")
                .contains(
                        "baseline_level",
                        "learning_goal",
                        "weak_point",
                        "preference",
                        "pace_and_feedback",
                        "recent_error_pattern",
                        "teacher_note"
                );
        assertThat(response.reasonSummary()).contains("Spring Boot APIs", "SQL JOIN reasoning", "RAG grounding");
    }

    @Test
    void generatesDeterministicLearningPathWithTraceableReasons() {
        TraceContext.setCurrentTraceId("trace-path");
        CreateLearningPathRequest request = new CreateLearningPathRequest("learner_sql", "goal_spring_boot");

        LearningPathResponse first = service.generatePath(request);
        LearningPathResponse second = service.generatePath(request);

        assertThat(second.pathId()).isEqualTo(first.pathId());
        assertThat(first.traceId()).isEqualTo("trace-path");
        assertThat(first.reasonSummary()).contains("goal_spring_boot", "learner_sql");
        assertThat(first.nodes()).hasSize(3);
        assertThat(first.nodes())
                .allSatisfy(node -> assertThat(node.reasonSummary()).contains("because"));
        assertThat(first.nodes().getFirst().getClass().getRecordComponents())
                .extracting(RecordComponent::getName)
                .contains(
                        "recommendationReason",
                        "estimatedDurationMinutes",
                        "resourceType",
                        "assessmentBindingRelation"
                );
        assertThat(service.getPath(first.pathId())).isEqualTo(second);
    }

    @Test
    void createPathForUserLegacyOverloadIsRemoved() {
        assertThat(Arrays.stream(LearningWorkflowService.class.getMethods())
                .noneMatch(method -> method.getName().equals("createPathForUser")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[]{
                        String.class,
                        CreateLearningPathRequest.class
                }))).isTrue();
    }

    @Test
    void learningWorkflowServiceSubjectNameAdminHelperIsRemoved() {
        assertThat(Arrays.stream(LearningWorkflowService.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().equals("isAdmin")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[]{String.class}))).isTrue();
    }

    @Test
    void createPathForUserRolesFirstOverloadAllowsExplicitAdminCrossLearnerCreation() {
        LearningPathResponse response = service.createPathForUser(
                "ops_admin",
                true,
                false,
                new CreateLearningPathRequest("alice", "goal_spring_boot")
        );

        assertThat(response.learnerId()).isEqualTo("alice");
        assertThat(response.goalId()).isEqualTo("goal_spring_boot");
    }
}
