package com.learningos.agent.application;

import org.springframework.ai.embedding.EmbeddingModel;

public interface EmbeddingModelFactory {

    EmbeddingModel createEmbeddingModel(String baseUrl, String apiKey, String embeddingModel);
}
