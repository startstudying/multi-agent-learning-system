package com.learningos.rag.vector;

import com.learningos.config.RagVectorProperties;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("external-smoke")
class QdrantVectorExternalSmokeTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "QDRANT_SMOKE_ENABLED", matches = "true")
    void optInQdrantSmokeChecksCollectionAndExpectedDimension() {
        RagVectorProperties properties = new RagVectorProperties(
                true,
                "qdrant",
                new RagVectorProperties.Qdrant(
                        env("RAG_VECTOR_QDRANT_HOST", "127.0.0.1"),
                        intEnv("RAG_VECTOR_QDRANT_PORT", 6334),
                        booleanEnv("RAG_VECTOR_QDRANT_TLS", false),
                        env("RAG_VECTOR_QDRANT_API_KEY", ""),
                        env("RAG_VECTOR_QDRANT_COLLECTION", "learning_os_chunks"),
                        false,
                        Duration.ofMillis(intEnv("RAG_VECTOR_QDRANT_TIMEOUT_MS", 3_000)),
                        intEnv("RAG_VECTOR_QDRANT_EXPECTED_DIMENSION", 0)
                )
        );
        properties.validateQdrant();

        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                properties.qdrant().host(),
                properties.qdrant().port(),
                properties.qdrant().useTls()
        ).withTimeout(properties.qdrant().timeout());
        if (!properties.qdrant().apiKey().isBlank()) {
            builder.withApiKey(properties.qdrant().apiKey());
        }
        QdrantClient client = new QdrantClient(builder.build());
        @SuppressWarnings("unchecked")
        ObjectProvider<QdrantClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);

        QdrantVectorHealthProbe.QdrantHealthSnapshot snapshot =
                new QdrantVectorHealthProbe(properties, provider).probe();

        assertThat(snapshot.status())
                .as("Qdrant smoke requires a reachable service and an existing collection. Metadata: %s", snapshot.metadata())
                .isEqualTo("UP");
        assertThat(snapshot.metadata())
                .containsEntry("collection", properties.qdrant().collectionName())
                .containsEntry("collectionExists", true);
        assertThat(snapshot.toString())
                .doesNotContain(properties.qdrant().apiKey(), "secret", "sk-");
    }

    private String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int intEnv(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }

    private boolean booleanEnv(String name, boolean fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
