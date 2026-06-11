package com.learningos.learning.application;

import com.learningos.learning.domain.LearningPath;
import com.learningos.learning.domain.LearningPathNode;
import com.learningos.learning.repository.LearningPathNodeRepository;
import com.learningos.learning.repository.LearningPathRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class LearningPathReplanService {

    public static final String STATUS_REPLAN_REQUIRED = "REPLAN_REQUIRED";
    public static final String STATUS_NO_REPLAN_REQUIRED = "NO_REPLAN_REQUIRED";
    private static final double COMPLETION_THRESHOLD = 0.80;

    private final LearningPathRepository learningPathRepository;
    private final LearningPathNodeRepository learningPathNodeRepository;

    public LearningPathReplanService(
            LearningPathRepository learningPathRepository,
            LearningPathNodeRepository learningPathNodeRepository
    ) {
        this.learningPathRepository = learningPathRepository;
        this.learningPathNodeRepository = learningPathNodeRepository;
    }

    @Transactional
    public LearningPathReplanDecision evaluateMasteryUpdate(
            String learnerId,
            String knowledgePointId,
            double beforeMastery,
            double afterMastery,
            String traceId
    ) {
        List<LearningPathNode> nodes = learningPathNodeRepository
                .findByLearnerIdAndKnowledgePointId(learnerId, knowledgePointId);
        if (nodes.isEmpty()) {
            return new LearningPathReplanDecision(
                    false,
                    STATUS_NO_REPLAN_REQUIRED,
                    List.of(),
                    "No active learning path nodes reference knowledge point " + knowledgePointId + ".",
                    traceId
            );
        }

        boolean replanRequired = shouldReplan(nodes, beforeMastery, afterMastery);
        Set<String> affectedPathIds = new LinkedHashSet<>();
        List<LearningPath> affectedPaths = new ArrayList<>();
        String reasonSummary = reasonSummary(knowledgePointId, beforeMastery, afterMastery, replanRequired);

        for (LearningPathNode node : nodes) {
            node.setMastery(afterMastery);
            if (replanRequired) {
                node.setStatus(STATUS_REPLAN_REQUIRED);
                node.setReasonSummary(reasonSummary);
                affectedPathIds.add(node.getPathId());
                learningPathRepository.findById(node.getPathId())
                        .ifPresent(path -> {
                            path.setStatus(STATUS_REPLAN_REQUIRED);
                            path.setReasonSummary(reasonSummary);
                            path.setTraceId(traceId);
                            affectedPaths.add(path);
                        });
            }
        }

        learningPathNodeRepository.saveAll(nodes);
        affectedPaths.forEach(learningPathRepository::save);

        return new LearningPathReplanDecision(
                replanRequired,
                replanRequired ? STATUS_REPLAN_REQUIRED : STATUS_NO_REPLAN_REQUIRED,
                List.copyOf(affectedPathIds),
                reasonSummary,
                traceId
        );
    }

    private boolean shouldReplan(List<LearningPathNode> nodes, double beforeMastery, double afterMastery) {
        if (crossedCompletionThreshold(beforeMastery, afterMastery)) {
            return true;
        }
        return nodes.stream().anyMatch(node -> statusContradictsMastery(node.getStatus(), afterMastery));
    }

    private boolean crossedCompletionThreshold(double beforeMastery, double afterMastery) {
        return beforeMastery >= COMPLETION_THRESHOLD && afterMastery < COMPLETION_THRESHOLD
                || beforeMastery < COMPLETION_THRESHOLD && afterMastery >= COMPLETION_THRESHOLD;
    }

    private boolean statusContradictsMastery(String status, double mastery) {
        return "DONE".equals(status) && mastery < COMPLETION_THRESHOLD
                || ("ACTIVE".equals(status) || "READY".equals(status)) && mastery >= COMPLETION_THRESHOLD;
    }

    private String reasonSummary(
            String knowledgePointId,
            double beforeMastery,
            double afterMastery,
            boolean replanRequired
    ) {
        if (replanRequired) {
            return "Mastery changed from " + beforeMastery + " to " + afterMastery
                    + " for " + knowledgePointId + "; the learning path requires replanning.";
        }
        return "Mastery changed from " + beforeMastery + " to " + afterMastery
                + " for " + knowledgePointId + " without crossing a replan threshold.";
    }
}
