package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learning-os.token-budget")
public record TokenBudgetProperties(
        boolean enforcementEnabled,
        long dailyLimit
) {
    public TokenBudgetProperties {
        if (dailyLimit <= 0) {
            dailyLimit = 10_000L;
        }
    }
}
