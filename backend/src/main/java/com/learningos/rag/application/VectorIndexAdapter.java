package com.learningos.rag.application;

public interface VectorIndexAdapter {

    boolean isEnabled();

    VectorUpsertResult deleteDocument(String kbId, String documentId, int documentVersion);

    VectorUpsertResult upsert(VectorUpsertRequest request);

    VectorSearchResult search(VectorSearchRequest request);
}
