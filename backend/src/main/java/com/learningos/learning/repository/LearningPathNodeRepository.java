package com.learningos.learning.repository;

import com.learningos.learning.domain.LearningPathNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningPathNodeRepository extends JpaRepository<LearningPathNode, String> {

    long countByPathId(String pathId);

    List<LearningPathNode> findByLearnerIdAndKnowledgePointId(String learnerId, String knowledgePointId);

    List<LearningPathNode> findByPathIdOrderBySequenceNoAsc(String pathId);
}
