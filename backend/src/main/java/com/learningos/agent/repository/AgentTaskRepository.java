package com.learningos.agent.repository;

import com.learningos.agent.domain.AgentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentTaskRepository extends JpaRepository<AgentTask, String> {

    Optional<AgentTask> findByOwnerUserIdAndInputJsonContaining(String ownerUserId, String inputJsonMarker);

    List<AgentTask> findByOwnerUserIdAndTaskTypeAndInputJsonContainingOrderByCreatedAtAsc(
            String ownerUserId,
            String taskType,
            String inputJsonMarker
    );
}
