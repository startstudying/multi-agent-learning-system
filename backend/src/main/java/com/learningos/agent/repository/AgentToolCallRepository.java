package com.learningos.agent.repository;

import com.learningos.agent.domain.AgentToolCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentToolCallRepository extends JpaRepository<AgentToolCall, String> {

    List<AgentToolCall> findByAgentTaskIdOrderByCreatedAtAsc(String agentTaskId);
}
