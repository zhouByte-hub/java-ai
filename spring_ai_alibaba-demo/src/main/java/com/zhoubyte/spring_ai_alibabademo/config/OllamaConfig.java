package com.zhoubyte.spring_ai_alibabademo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 使用 Spring AI 提供的 Ollama ChatModel，
 * 再通过 ChatClient 统一对外提供调用接口。
 */
@Configuration
public class OllamaConfig {

    /**
     * 基于本地 Ollama ChatModel 创建 ChatClient。
     * ChatModel 由 spring-ai-starter-model-ollama 根据 spring.ai.ollama.* 配置自动装配。
     */
    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

