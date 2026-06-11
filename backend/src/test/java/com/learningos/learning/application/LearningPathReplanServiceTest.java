package com.learningos.learning.application;

import com.learningos.learning.domain.LearningPath;
import com.learningos.learning.domain.LearningPathNode;
import com.learningos.learning.repository.LearningPathNodeRepository;
import com.learningos.learning.repository.LearningPathRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningPathReplanServiceTest {

    private final LearningPathRepository pathRepository = mock(LearningPathRepository.class);
    private final LearningPathNodeRepository nodeRepository = mock(LearningPathNodeRepository.class);
    private final LearningPathReplanService service = new LearningPathReplanService(pathRepository, nodeRepository);

    @Test
    void marksPathAndNodeReplanRequiredWhenUpdatedMasteryInvalidatesCurrentNode() {
        LearningPath path = path("path_1", "ACTIVE");
        LearningPathNode node = node("node_1", "path_1", "learner_1", "kp_join", "DONE", 0.86);
        when(nodeRepository.findByLearnerIdAndKnowledgePointId("learner_1", "kp_join"))
                .thenReturn(List.of(node));
        when(pathRepository.findById("path_1")).thenReturn(Optional.of(path));

        LearningPathReplanDecision decision = service.evaluateMasteryUpdate(
                "learner_1",
                "kp_join",
                0.86,
                0.52,
                "trace_replan"
        );

        assertThat(decision.replanRequired()).isTrue();
        assertThat(decision.status()).isEqualTo("REPLAN_REQUIRED");
        assertThat(decision.affectedPathIds()).containsExactly("path_1");
        assertThat(node.getStatus()).isEqualTo("REPLAN_REQUIRED");
        assertThat(node.getMastery()).isEqualTo(0.52);
        assertThat(node.getReasonSummary()).contains("Mastery changed from 0.86 to 0.52");
        assertThat(path.getStatus()).isEqualTo("REPLAN_REQUIRED");
        assertThat(path.getReasonSummary()).contains("requires replanning", "kp_join");
        verify(nodeRepository).saveAll(List.of(node));
        verify(pathRepository).save(path);
    }

    @Test
    void updatesMasteryWithoutReplanWhenThresholdStateDoesNotChange() {
        LearningPath path = path("path_1", "ACTIVE");
        LearningPathNode node = node("node_1", "path_1", "learner_1", "kp_join", "ACTIVE", 0.42);
        when(nodeRepository.findByLearnerIdAndKnowledgePointId("learner_1", "kp_join"))
                .thenReturn(List.of(node));

        LearningPathReplanDecision decision = service.evaluateMasteryUpdate(
                "learner_1",
                "kp_join",
                0.42,
                0.58,
                "trace_replan"
        );

        assertThat(decision.replanRequired()).isFalse();
        assertThat(decision.status()).isEqualTo("NO_REPLAN_REQUIRED");
        assertThat(decision.affectedPathIds()).isEmpty();
        assertThat(node.getStatus()).isEqualTo("ACTIVE");
        assertThat(node.getMastery()).isEqualTo(0.58);
        verify(nodeRepository).saveAll(List.of(node));
        verify(pathRepository, never()).save(path);
    }

    private LearningPath path(String id, String status) {
        LearningPath path = new LearningPath();
        path.setId(id);
        path.setLearnerId("learner_1");
        path.setGoalId("goal_1");
        path.setStatus(status);
        path.setReasonSummary("Original path reason.");
        path.setTraceId("trace_path");
        return path;
    }

    private LearningPathNode node(
            String id,
            String pathId,
            String learnerId,
            String knowledgePointId,
            String status,
            double mastery
    ) {
        LearningPathNode node = new LearningPathNode();
        node.setId(id);
        node.setPathId(pathId);
        node.setLearnerId(learnerId);
        node.setKnowledgePointId(knowledgePointId);
        node.setTitle("SQL JOIN diagnosis");
        node.setStatus(status);
        node.setMastery(mastery);
        node.setReasonSummary("Original node reason.");
        node.setSequenceNo(1);
        return node;
    }
}
