package com.learningos.rag.vector;

import java.util.List;

public interface QdrantVectorOperations {

    void deleteDocument(QdrantVectorDeleteCommand command);

    void upsert(QdrantVectorUpsertCommand command);

    List<QdrantVectorSearchHit> search(QdrantVectorSearchCommand command);
}
