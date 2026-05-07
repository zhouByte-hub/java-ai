package org.zhoubyte.mcpserver.stdio;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StdioMCPConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(ZoomTool zoomTool) {
        return MethodToolCallbackProvider.builder().toolObjects(zoomTool).build();
    }
}
