package com.learningos.rag.application;

public class NoopVectorIndexAdapter implements VectorIndexAdapter {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public VectorUpsertResult deleteDocument(String kbId, String documentId, int documentVersion) {
        return VectorUpsertResult.disabled();
    }

    @Override
    public VectorUpsertResult upsert(VectorUpsertRequest request) {
        return VectorUpsertResult.disabled();
    }

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        return VectorSearchResult.disabled();
    }
}
