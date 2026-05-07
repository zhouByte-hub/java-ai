package org.zhoubyte.alibabamcpclient.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlibabaMcpClientConfig {

    @Bean
    public ChatClient mcpChatClient(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel).defaultToolCallbacks(toolCallbackProvider.getToolCallbacks()).build();
    }
}
