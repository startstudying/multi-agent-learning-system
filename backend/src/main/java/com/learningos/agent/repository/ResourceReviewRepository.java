package com.learningos.agent.repository;

import com.learningos.agent.domain.ResourceReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceReviewRepository extends JpaRepository<ResourceReview, String> {

    long countByGenerationTaskId(String generationTaskId);

    long countByGenerationTaskIdAndStatus(String generationTaskId, String status);

    List<ResourceReview> findByGenerationTaskIdOrderByCreatedAtAsc(String generationTaskId);

    List<ResourceReview> findByStatusOrderByCreatedAtAsc(String status);
}
