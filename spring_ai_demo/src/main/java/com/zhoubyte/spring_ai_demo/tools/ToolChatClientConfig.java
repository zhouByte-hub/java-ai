package com.zhoubyte.spring_ai_demo.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolChatClientConfig {

    @Bean
    public ChatClient toolChatClient(ChatModel chatModel, ZoomTool zoomTool) {
        return ChatClient.builder(chatModel)
                .defaultSystem("拿铁咖啡制作需要一分钟，美式咖啡制作需要 1-2分钟，蜜雪冰城出门右转。")
                .defaultTools(zoomTool)
                .build();
    }
}
