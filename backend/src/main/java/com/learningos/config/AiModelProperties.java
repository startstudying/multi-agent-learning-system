package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learning-os.ai-model")
public record AiModelProperties(
        String provider,
        String chatModel,
        String embeddingModel
) {
    public AiModelProperties {
        if (provider == null || provider.isBlank()) {
            provider = "none";
        }
    }

    public boolean configured() {
        return !"none".equalsIgnoreCase(provider);
    }
}
