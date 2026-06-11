package com.learningos.agent.application;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleEmbeddingModelFactory implements EmbeddingModelFactory {

    @Override
    public EmbeddingModel createEmbeddingModel(String baseUrl, String apiKey, String embeddingModel) {
        String normalizedBaseUrl = OpenAiCompatibleChatModelFactory.normalizeBaseUrl(baseUrl);
        requiredText(embeddingModel, "Embedding model is required");
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizedBaseUrl)
                .apiKey(requiredText(apiKey, "API key is required"))
                .build();
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED);
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
