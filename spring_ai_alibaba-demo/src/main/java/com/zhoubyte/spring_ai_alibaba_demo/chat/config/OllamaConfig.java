package com.zhoubyte.spring_ai_alibaba_demo.chat.config;

import com.zhoubyte.spring_ai_alibaba_demo.chat.adviser.SimpleMemories;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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
    public ChatClient ollamaChatClient(ChatModel chatModel, BaseAdvisor memoriesAdvisor, SimpleMemories simpleMemories, ChatMemoryRepository databaseChatMemoryRepository) {
        // 大模型记忆：方式一
//        ChatMemory.CONVERSATION_ID
        MessageWindowChatMemory build = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .chatMemoryRepository(databaseChatMemoryRepository)
                .build();


        MessageChatMemoryAdvisor chatMemoriesSessionId = MessageChatMemoryAdvisor
                .builder(simpleMemories)    // 使用 simpleMemories或者使用 build
                .conversationId("chat_memories_session_id")
                .order(1)
                .build();

        // 大模型记忆：方式二
        return ChatClient.builder(chatModel)
                .defaultAdvisors(memoriesAdvisor)
                .build();
    }
}

