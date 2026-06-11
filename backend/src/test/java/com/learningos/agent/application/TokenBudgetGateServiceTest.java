package com.learningos.agent.application;

import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.config.TokenBudgetProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenBudgetGateServiceTest {

    @Test
    void returnsNormalStrategyWhenWithinBudget() {
        TokenUsageLogRepository repository = mock(TokenUsageLogRepository.class);
        when(repository.sumTotalTokens()).thenReturn(2_000L);
        TokenBudgetGateService service = new TokenBudgetGateService(
                new TokenBudgetProperties(true, 10_000L),
                repository
        );

        TokenBudgetGateService.TokenBudgetDecision decision = service.evaluate();

        assertThat(decision.overBudget()).isFalse();
        assertThat(decision.fallbackStrategy()).isEqualTo("NORMAL");
        assertThat(decision.remaining()).isEqualTo(8_000L);
    }

    @Test
    void returnsDeterministicFallbackWhenOverBudget() {
        TokenUsageLogRepository repository = mock(TokenUsageLogRepository.class);
        when(repository.sumTotalTokens()).thenReturn(12_000L);
        TokenBudgetGateService service = new TokenBudgetGateService(
                new TokenBudgetProperties(true, 10_000L),
                repository
        );

        TokenBudgetGateService.TokenBudgetDecision decision = service.evaluate();

        assertThat(decision.overBudget()).isTrue();
        assertThat(decision.fallbackStrategy()).isEqualTo(TokenBudgetGateService.FALLBACK_DETERMINISTIC_ONLY);
    }
}
