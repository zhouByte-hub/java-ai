package com.zhoubyte.spring_ai_demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    /**
     * 创建 ChatClient调用大模型，ChatClient是比ChatModel更高一级的封装
     * @param chatModel Ollama提供的大模型对象
     * @return ChatClient
     */
    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }

}
