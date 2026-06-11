package com.learningos.agent.application;

import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.config.TokenBudgetProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenBudgetGateService {

    public static final String FALLBACK_DETERMINISTIC_ONLY = "DETERMINISTIC_ONLY";

    private final TokenBudgetProperties properties;
    private final TokenUsageLogRepository tokenUsageLogRepository;

    public TokenBudgetGateService(TokenBudgetProperties properties, TokenUsageLogRepository tokenUsageLogRepository) {
        this.properties = properties;
        this.tokenUsageLogRepository = tokenUsageLogRepository;
    }

    @Transactional(readOnly = true)
    public TokenBudgetDecision evaluate() {
        long totalTokens = tokenUsageLogRepository.sumTotalTokens();
        long remaining = Math.max(0L, properties.dailyLimit() - totalTokens);
        boolean overBudget = totalTokens >= properties.dailyLimit();
        if (!properties.enforcementEnabled()) {
            return new TokenBudgetDecision(false, totalTokens, properties.dailyLimit(), remaining, "NORMAL");
        }
        String strategy = overBudget ? FALLBACK_DETERMINISTIC_ONLY : "NORMAL";
        return new TokenBudgetDecision(overBudget, totalTokens, properties.dailyLimit(), remaining, strategy);
    }

    public record TokenBudgetDecision(
            boolean overBudget,
            long totalTokens,
            long budgetLimit,
            long remaining,
            String fallbackStrategy
    ) {
    }
}
