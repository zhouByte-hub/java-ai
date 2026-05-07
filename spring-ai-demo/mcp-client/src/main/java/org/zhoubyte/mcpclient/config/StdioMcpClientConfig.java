package org.zhoubyte.mcpclient.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StdioMcpClientConfig {

    @Bean
    public ChatClient stdioMcpChatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
        return chatClientBuilder.defaultToolCallbacks(toolCallbackProvider.getToolCallbacks()).build();
    }
}
