package org.zhoubyte.alibabamcpserver.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(AlibabaMcpTools mcpTools) {
        return MethodToolCallbackProvider.builder().toolObjects(mcpTools).build();
    }
}
