package com.learningos.rag.vector;

import com.learningos.config.RagVectorProperties;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class QdrantVectorHealthProbe {

    private final RagVectorProperties properties;
    private final ObjectProvider<QdrantClient> qdrantClientProvider;

    public QdrantVectorHealthProbe(
            RagVectorProperties properties,
            ObjectProvider<QdrantClient> qdrantClientProvider
    ) {
        this.properties = properties;
        this.qdrantClientProvider = qdrantClientProvider;
    }

    public QdrantHealthSnapshot probe() {
        if (!properties.qdrantEnabled()) {
            return QdrantHealthSnapshot.disabled("qdrant vector index disabled");
        }
        QdrantClient client = qdrantClientProvider.getIfAvailable();
        if (client == null) {
            return QdrantHealthSnapshot.down("QDRANT_CLIENT_UNAVAILABLE", Map.of(
                    "provider", properties.provider(),
                    "host", properties.qdrant().host(),
                    "port", properties.qdrant().port()
            ));
        }
        try {
            long timeoutMs = Math.max(500L, properties.qdrant().timeout().toMillis());
            var collections = client.listCollectionsAsync()
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            String collectionName = properties.qdrant().collectionName();
            boolean collectionExists = collections.stream().anyMatch(collectionName::equals);
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("provider", properties.provider());
            metadata.put("host", properties.qdrant().host());
            metadata.put("port", properties.qdrant().port());
            metadata.put("collection", collectionName);
            metadata.put("collectionExists", collectionExists);
            metadata.put("collectionCount", collections.size());
            if (!collectionExists) {
                metadata.put("errorCode", "COLLECTION_MISSING");
                return new QdrantHealthSnapshot("DOWN", "qdrant collection missing", metadata);
            }
            int expectedDimension = properties.qdrant().expectedDimension();
            if (expectedDimension > 0) {
                Collections.CollectionInfo info = client.getCollectionInfoAsync(collectionName)
                        .get(timeoutMs, TimeUnit.MILLISECONDS);
                int actualDimension = Math.toIntExact(info.getConfig().getParams().getVectorsConfig()
                        .getParams().getSize());
                metadata.put("expectedDimension", expectedDimension);
                metadata.put("actualDimension", actualDimension);
                if (actualDimension != expectedDimension) {
                    metadata.put("errorCode", "DIMENSION_MISMATCH");
                    return new QdrantHealthSnapshot(
                            "DOWN",
                            "qdrant collection dimension mismatch",
                            metadata
                    );
                }
            }
            return new QdrantHealthSnapshot("UP", "qdrant vector index reachable", metadata);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return QdrantHealthSnapshot.down("INTERRUPTED", Map.of("provider", properties.provider()));
        } catch (Exception ex) {
            return QdrantHealthSnapshot.down("PROBE_FAILED", Map.of(
                    "provider", properties.provider(),
                    "errorCode", "PROBE_FAILED"
            ));
        }
    }

    public record QdrantHealthSnapshot(String status, String detail, Map<String, Object> metadata) {
        public static QdrantHealthSnapshot disabled(String detail) {
            return new QdrantHealthSnapshot("DISABLED", detail, Map.of("provider", "none"));
        }

        public static QdrantHealthSnapshot down(String errorCode, Map<String, Object> metadata) {
            Map<String, Object> merged = new LinkedHashMap<>(metadata);
            merged.putIfAbsent("errorCode", errorCode);
            return new QdrantHealthSnapshot("DOWN", "qdrant health probe failed", merged);
        }
    }
}
