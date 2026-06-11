package com.learningos.rag.vector;

import com.learningos.config.RagVectorProperties;
import com.learningos.rag.application.EmbeddingVector;
import com.learningos.rag.application.VectorChunkReference;
import com.learningos.rag.application.VectorIndexAdapter;
import com.learningos.rag.application.VectorIndexStatus;
import com.learningos.rag.application.VectorSearchHit;
import com.learningos.rag.application.VectorSearchRequest;
import com.learningos.rag.application.VectorSearchResult;
import com.learningos.rag.application.VectorUpsertRequest;
import com.learningos.rag.application.VectorUpsertResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class QdrantVectorIndexAdapter implements VectorIndexAdapter {

    private static final String PROVIDER_NOT_CONFIGURED = "VECTOR_PROVIDER_NOT_CONFIGURED";
    private static final String DELETE_FAILED = "VECTOR_DELETE_FAILED";
    private static final String UPSERT_FAILED = "VECTOR_UPSERT_FAILED";
    private static final String SEARCH_FAILED = "VECTOR_SEARCH_FAILED";

    private final RagVectorProperties properties;
    private final QdrantVectorOperations operations;

    public QdrantVectorIndexAdapter(RagVectorProperties properties, QdrantVectorOperations operations) {
        this.properties = properties;
        this.operations = operations;
        this.properties.validateQdrant();
    }

    @Override
    public boolean isEnabled() {
        return properties.qdrantEnabled();
    }

    @Override
    public VectorUpsertResult deleteDocument(String kbId, String documentId, int documentVersion) {
        if (!isEnabled()) {
            return VectorUpsertResult.disabled();
        }
        long startedAt = System.nanoTime();
        try {
            operations.deleteDocument(new QdrantVectorDeleteCommand(
                    properties.qdrant().collectionName(),
                    kbId,
                    documentId,
                    documentVersion,
                    properties.qdrant().timeout()
            ));
            return new VectorUpsertResult(VectorIndexStatus.SUCCEEDED, 0, latencyMs(startedAt), null);
        } catch (RuntimeException exception) {
            return new VectorUpsertResult(VectorIndexStatus.VECTOR_UPSERT_FAILED, 0, latencyMs(startedAt), DELETE_FAILED);
        }
    }

    @Override
    public VectorUpsertResult upsert(VectorUpsertRequest request) {
        if (!isEnabled()) {
            return VectorUpsertResult.disabled();
        }
        long startedAt = System.nanoTime();
        try {
            List<QdrantVectorPoint> points = toPoints(request);
            operations.upsert(new QdrantVectorUpsertCommand(
                    properties.qdrant().collectionName(),
                    points,
                    properties.qdrant().timeout()
            ));
            return new VectorUpsertResult(VectorIndexStatus.SUCCEEDED, points.size(), latencyMs(startedAt), null);
        } catch (RuntimeException exception) {
            return new VectorUpsertResult(VectorIndexStatus.VECTOR_UPSERT_FAILED, 0, latencyMs(startedAt), UPSERT_FAILED);
        }
    }

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        if (!isEnabled()) {
            return VectorSearchResult.disabled();
        }
        long startedAt = System.nanoTime();
        try {
            if (request == null || request.queryVector() == null || request.queryVector().dimension() == 0) {
                return new VectorSearchResult(VectorIndexStatus.PROVIDER_ERROR, List.of(), latencyMs(startedAt), PROVIDER_NOT_CONFIGURED);
            }
            List<QdrantVectorSearchHit> hits = operations.search(new QdrantVectorSearchCommand(
                    properties.qdrant().collectionName(),
                    request.allowedKbIds(),
                    request.topK(),
                    request.queryVector().values(),
                    properties.qdrant().timeout()
            ));
            return new VectorSearchResult(
                    VectorIndexStatus.SUCCEEDED,
                    hits.stream()
                            .filter(hit -> hit != null && hit.chunkId() != null && !hit.chunkId().isBlank())
                            .map(hit -> new VectorSearchHit(hit.chunkId(), hit.score()))
                            .toList(),
                    latencyMs(startedAt),
                    null
            );
        } catch (RuntimeException exception) {
            return new VectorSearchResult(VectorIndexStatus.PROVIDER_ERROR, List.of(), latencyMs(startedAt), SEARCH_FAILED);
        }
    }

    private List<QdrantVectorPoint> toPoints(VectorUpsertRequest request) {
        if (request == null || request.chunks() == null) {
            return List.of();
        }
        return request.chunks().stream()
                .filter(reference -> reference != null && hasUsableVector(reference.vector()))
                .map(reference -> new QdrantVectorPoint(
                        reference.chunkId(),
                        request.kbId(),
                        request.documentId(),
                        request.documentVersion(),
                        reference.chunkHash(),
                        reference.chunkIndex(),
                        reference.vector().values()
                ))
                .toList();
    }

    private boolean hasUsableVector(EmbeddingVector vector) {
        return vector != null && vector.dimension() > 0;
    }

    private long latencyMs(long startedAt) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }
}
