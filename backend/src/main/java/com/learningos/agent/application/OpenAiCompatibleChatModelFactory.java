package com.learningos.agent.application;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class OpenAiCompatibleChatModelFactory implements ChatModelFactory {

    public ChatModel createChatModel(String baseUrl, String apiKey, String chatModel) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        String normalizedModel = requiredText(chatModel, "Chat model is required");
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizedBaseUrl)
                .apiKey(requiredText(apiKey, "API key is required"))
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(normalizedModel)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    static String normalizeBaseUrl(String baseUrl) {
        String trimmed = requiredText(baseUrl, "Base URL is required");
        URI uri = URI.create(trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed);
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
            throw new IllegalArgumentException("Base URL must be a valid http(s) URL");
        }
        return uri.toString();
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
