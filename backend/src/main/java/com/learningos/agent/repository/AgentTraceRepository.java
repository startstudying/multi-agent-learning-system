package com.learningos.agent.repository;

import com.learningos.agent.domain.AgentTrace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTraceRepository extends JpaRepository<AgentTrace, String> {

    long countByAgentTaskId(String agentTaskId);

    List<AgentTrace> findByAgentTaskIdOrderBySequenceNoAsc(String agentTaskId);

    void deleteByAgentTaskId(String agentTaskId);
}
