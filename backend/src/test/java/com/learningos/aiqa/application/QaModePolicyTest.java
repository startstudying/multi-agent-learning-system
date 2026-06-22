package com.learningos.aiqa.application;

import org.junit.jupiter.api.Test;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QaModePolicyTest {

    private final QaModePolicy policy = new QaModePolicy();

    @Test
    void mapsAnswerModesToReasoningEffort() {
        assertThat(policy.resolve("FAST").reasoningEffort()).isEqualTo("low");
        assertThat(policy.resolve("THINKING").reasoningEffort()).isEqualTo("medium");
        assertThat(policy.resolve("EXPERT").reasoningEffort()).isEqualTo("high");
    }

    @Test
    void defaultsBlankModeToThinking() {
        QaModePolicy.QaModeStrategy strategy = policy.resolve(" ");

        assertThat(strategy.answerMode()).isEqualTo("THINKING");
        assertThat(strategy.reasoningEffort()).isEqualTo("medium");
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> policy.resolve("XHIGH"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void createsSafeSummaryWithoutInternalReasoning() {
        QaModePolicy.QaModeStrategy strategy = policy.resolve("EXPERT");

        assertThat(strategy.safeReasoningSummary(2, false))
                .contains("EXPERT")
                .contains("high")
                .doesNotContain("chain-of-thought")
                .doesNotContain("system prompt")
                .doesNotContain("API key");
    }

    @Test
    void createsSafeFallbackSummaryWithoutInternalReasoning() {
        QaModePolicy.QaModeStrategy strategy = policy.resolve("FAST");

        assertThat(strategy.safeReasoningSummary(0, true))
                .contains("FAST")
                .contains("low")
                .doesNotContain("chain-of-thought")
                .doesNotContain("system prompt")
                .doesNotContain("API key");
    }
}
