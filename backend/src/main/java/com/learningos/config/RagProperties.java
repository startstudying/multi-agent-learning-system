package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learning-os.rag")
public record RagProperties(
        int defaultTopK,
        int maxContextTokens,
        long rerankerTimeoutMs
) {
    public RagProperties {
        if (defaultTopK <= 0) {
            defaultTopK = 20;
        }
        if (maxContextTokens <= 0) {
            maxContextTokens = 6000;
        }
        if (rerankerTimeoutMs <= 0) {
            rerankerTimeoutMs = 2000;
        }
    }
}
