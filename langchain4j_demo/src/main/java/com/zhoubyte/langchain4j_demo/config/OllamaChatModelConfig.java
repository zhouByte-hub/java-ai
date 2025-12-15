package com.zhoubyte.langchain4j_demo.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaChatModelConfig {

    @Bean
    public ChatModel ollamaChatModel() {
        return new OllamaChatModel.OllamaChatModelBuilder()
                .modelName("qwen3:0.6b")
                .maxRetries(5)
                .baseUrl("http://localhost:11434")
                .returnThinking(true)
                .temperature(0.8)
                .build();
    }
}
