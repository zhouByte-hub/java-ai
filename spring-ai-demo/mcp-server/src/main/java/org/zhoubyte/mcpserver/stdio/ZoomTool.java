package org.zhoubyte.mcpserver.stdio;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


/**
 * @McpTool: 将方法标记为 MCP 工具，并自动生成 JSON 模式
 * @McpResource - 通过 URI 模板提供资源访问权限
 * @McpPrompt - 为 AI 交互生成提示消息
 * @McpComplete - 为提示提供自动补全功能
 */
@Component
public class ZoomTool {

    // 使用@Tool或者@McpTool注解都可以
    @McpTool(name = "getTimeByZone", description = "通过时区 ID 获取当前时间")
    public String getTimeByZone(@McpToolParam(description = "时区 ID，比如 Asia/Shanghai") String zone) {
        ZoneId zoneId = ZoneId.of(zone);
        ZonedDateTime zoneddateTime = ZonedDateTime.now(zoneId);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zoneddateTime);
    }

    @Tool(name = "getCurrentUser", description = "获取当前登录用户的用户名称")
    public String getCurrentUser(){
        return "ZhouByte";
    }
}
