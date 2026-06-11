package com.learningos.agent.application;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("external-smoke")
class ModelProviderExternalSmokeTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "MODEL_PROVIDER_SMOKE_ENABLED", matches = "true")
    void optInExternalProviderSmokeCallsConfiguredOpenAiCompatibleChatEndpoint() {
        String providerCode = env("MODEL_PROVIDER_CODE", "custom");
        String chatModelName = env("MODEL_PROVIDER_CHAT_MODEL", "");
        ChatResponse response = new OpenAiCompatibleChatModelFactory()
                .createChatModel(
                        env("MODEL_PROVIDER_BASE_URL", ""),
                        env("MODEL_PROVIDER_API_KEY", ""),
                        chatModelName
                )
                .call(new Prompt(new UserMessage("Reply with the single word pong.")));

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getOutput()).isNotNull();
        assertThat(response.getResult().getOutput().getText()).isNotBlank();
        if (response.getMetadata() != null && response.getMetadata().getModel() != null) {
            assertThat(response.getMetadata().getModel())
                    .doesNotContain("sk-", "apiKey", "secret", "http://", "https://");
        }
        assertThat(providerCode).isNotBlank();
        assertThat(chatModelName).isNotBlank();
    }

    private String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
