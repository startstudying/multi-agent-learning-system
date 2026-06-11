package com.learningos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;

@ConfigurationProperties(prefix = "learning-os.rag.vector")
public record RagVectorProperties(
        boolean enabled,
        String provider,
        Qdrant qdrant
) {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);

    @ConstructorBinding
    public RagVectorProperties {
        provider = provider == null || provider.isBlank() ? "none" : provider.trim();
        qdrant = qdrant == null
                ? new Qdrant("", 6334, false, "", "learning_os_chunks", false, DEFAULT_TIMEOUT, 0)
                : qdrant;
    }

    public boolean qdrantEnabled() {
        return enabled && "qdrant".equalsIgnoreCase(provider);
    }

    public void validateQdrant() {
        if (!qdrantEnabled()) {
            return;
        }
        if (qdrant.host().isBlank()) {
            throw new IllegalStateException("RAG_VECTOR_QDRANT_HOST is required when qdrant vector is enabled");
        }
        if (qdrant.port() <= 0 || qdrant.port() > 65535) {
            throw new IllegalStateException("RAG_VECTOR_QDRANT_PORT is invalid");
        }
        if (qdrant.collectionName().isBlank()) {
            throw new IllegalStateException("RAG_VECTOR_QDRANT_COLLECTION is required when qdrant vector is enabled");
        }
    }

    public record Qdrant(
            String host,
            int port,
            boolean useTls,
            String apiKey,
            String collectionName,
            boolean initializeSchema,
            Duration timeout,
            int expectedDimension
    ) {
        public Qdrant {
            host = host == null ? "" : host.trim();
            apiKey = apiKey == null ? "" : apiKey.trim();
            collectionName = collectionName == null || collectionName.isBlank()
                    ? "learning_os_chunks"
                    : collectionName.trim();
            timeout = timeout == null || timeout.isZero() || timeout.isNegative()
                    ? DEFAULT_TIMEOUT
                    : timeout;
            if (expectedDimension < 0) {
                expectedDimension = 0;
            }
        }
    }
}
