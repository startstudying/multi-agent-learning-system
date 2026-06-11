package com.learningos.agent.repository;

import com.learningos.agent.domain.TokenUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TokenUsageLogRepository extends JpaRepository<TokenUsageLog, String> {

    long countByTraceId(String traceId);

    @Query("select coalesce(sum(t.promptTokens), 0) from TokenUsageLog t")
    long sumPromptTokens();

    @Query("select coalesce(sum(t.completionTokens), 0) from TokenUsageLog t")
    long sumCompletionTokens();

    @Query("select coalesce(sum(t.totalTokens), 0) from TokenUsageLog t")
    long sumTotalTokens();

    @Query("select coalesce(sum(t.estimatedCost), 0.0) from TokenUsageLog t")
    double sumEstimatedCost();

    @Query("""
            select
                t.agentTaskId as agentTaskId,
                coalesce(sum(t.promptTokens), 0) as promptTokens,
                coalesce(sum(t.completionTokens), 0) as completionTokens,
                coalesce(sum(t.totalTokens), 0) as totalTokens,
                coalesce(sum(t.estimatedCost), 0.0) as estimatedCost
            from TokenUsageLog t
            group by t.agentTaskId
            order by t.agentTaskId
            """)
    java.util.List<AgentTaskTokenUsageSummary> summarizeByAgentTask();

    interface AgentTaskTokenUsageSummary {
        String getAgentTaskId();

        long getPromptTokens();

        long getCompletionTokens();

        long getTotalTokens();

        double getEstimatedCost();
    }
}
