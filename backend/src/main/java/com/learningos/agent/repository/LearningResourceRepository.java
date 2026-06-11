package com.learningos.agent.repository;

import com.learningos.agent.domain.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningResourceRepository extends JpaRepository<LearningResource, String> {

    long countByGenerationTaskId(String generationTaskId);

    long countByGenerationTaskIdAndReviewStatus(String generationTaskId, String reviewStatus);

    List<LearningResource> findByGenerationTaskIdOrderByCreatedAtAsc(String generationTaskId);
}
