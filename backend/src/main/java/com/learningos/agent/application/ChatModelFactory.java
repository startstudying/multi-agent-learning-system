package com.learningos.agent.application;

import org.springframework.ai.chat.model.ChatModel;

public interface ChatModelFactory {

    ChatModel createChatModel(String baseUrl, String apiKey, String chatModel);
}
