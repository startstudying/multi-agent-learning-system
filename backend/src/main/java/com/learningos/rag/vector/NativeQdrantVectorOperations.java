package com.learningos.rag.vector;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class NativeQdrantVectorOperations implements QdrantVectorOperations {

    private final QdrantClient client;

    public NativeQdrantVectorOperations(QdrantClient client) {
        this.client = client;
    }

    @Override
    public void deleteDocument(QdrantVectorDeleteCommand command) {
        Points.Filter filter = Points.Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword("kbId", command.kbId()))
                .addMust(ConditionFactory.matchKeyword("documentId", command.documentId()))
                .addMust(ConditionFactory.match("documentVersion", command.documentVersion()))
                .build();
        await(client.deleteAsync(command.collectionName(), filter, command.timeout()));
    }

    @Override
    public void upsert(QdrantVectorUpsertCommand command) {
        List<Points.PointStruct> points = command.points().stream()
                .map(this::toPointStruct)
                .toList();
        if (!points.isEmpty()) {
            await(client.upsertAsync(command.collectionName(), points, command.timeout()));
        }
    }

    @Override
    public List<QdrantVectorSearchHit> search(QdrantVectorSearchCommand command) {
        Points.SearchPoints request = Points.SearchPoints.newBuilder()
                .setCollectionName(command.collectionName())
                .addAllVector(toFloatList(command.queryVector()))
                .setFilter(Points.Filter.newBuilder()
                        .addMust(ConditionFactory.matchKeywords("kbId", command.allowedKbIds()))
                        .build())
                .setLimit(command.topK())
                .setWithPayload(Points.WithPayloadSelector.newBuilder()
                        .setInclude(Points.PayloadIncludeSelector.newBuilder().addFields("chunkId"))
                        .build())
                .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(false).build())
                .build();
        return await(client.searchAsync(request, command.timeout())).stream()
                .map(point -> new QdrantVectorSearchHit(
                        point.getPayloadOrDefault("chunkId", ValueFactory.value("")).getStringValue(),
                        point.getScore()
                ))
                .filter(hit -> hit.chunkId() != null && !hit.chunkId().isBlank())
                .toList();
    }

    private Points.PointStruct toPointStruct(QdrantVectorPoint point) {
        return Points.PointStruct.newBuilder()
                .setId(PointIdFactory.id(stableUuid(point.chunkId())))
                .setVectors(VectorsFactory.vectors(point.vector()))
                .putAllPayload(payload(point))
                .build();
    }

    private Map<String, JsonWithInt.Value> payload(QdrantVectorPoint point) {
        Map<String, JsonWithInt.Value> payload = new LinkedHashMap<>();
        payload.put("chunkId", ValueFactory.value(point.chunkId()));
        payload.put("kbId", ValueFactory.value(point.kbId()));
        payload.put("documentId", ValueFactory.value(point.documentId()));
        payload.put("documentVersion", ValueFactory.value(point.documentVersion()));
        payload.put("chunkHash", ValueFactory.value(point.chunkHash()));
        if (point.chunkIndex() != null) {
            payload.put("chunkIndex", ValueFactory.value(point.chunkIndex()));
        }
        return payload;
    }

    private UUID stableUuid(String value) {
        return UUID.nameUUIDFromBytes((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private List<Float> toFloatList(float[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        java.util.ArrayList<Float> floats = new java.util.ArrayList<>(values.length);
        for (float value : values) {
            floats.add(value);
        }
        return floats;
    }

    private <T> T await(com.google.common.util.concurrent.ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("QDRANT_OPERATION_INTERRUPTED");
        } catch (ExecutionException exception) {
            throw new IllegalStateException("QDRANT_OPERATION_FAILED");
        }
    }
}
