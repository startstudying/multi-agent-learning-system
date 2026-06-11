package com.learningos.agent.repository;

import com.learningos.agent.domain.ResourceGenerationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceGenerationTaskRepository extends JpaRepository<ResourceGenerationTask, String> {

    Optional<ResourceGenerationTask> findByLearnerIdAndRequestId(String learnerId, String requestId);
}
