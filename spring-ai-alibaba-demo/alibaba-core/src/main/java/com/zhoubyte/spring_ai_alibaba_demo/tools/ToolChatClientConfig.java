package com.zhoubyte.spring_ai_alibaba_demo.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ToolChatClientConfig {

    @Bean("toolChatClient")
    public ChatClient toolChatClient(ChatModel ollamaChatModel, ZoomTool zoomTool) {
        return ChatClient.builder(ollamaChatModel)
                .defaultSystem(this.systemPrompt())
                .defaultTools(zoomTool)
                .build();
    }

    private String systemPrompt()  {
        Map<String, Object> vars = new HashMap<>();
        vars.put("AMERICAN", "1-3");
        vars.put("LATTE", "2");
        vars.put("TIME_ZONE", "Asia/Shanghai");

        SystemPromptTemplate systemPromptTemplate = SystemPromptTemplate.builder()
                .template("欢迎光临 ZhouByte咖啡馆，馆内售卖美式咖啡和拿铁咖啡，美式咖啡的制作需要{AMERICAN}分钟，拿铁咖啡的制作需要{LATTE}分钟左右；" +
                        "如果你想喝柠檬水，你可以出门右转直走 150米左右有蜜雪冰城；默认时区：{TIME_ZONE}")
                .variables(vars)
                .build();
        return systemPromptTemplate.render();
    }
}
