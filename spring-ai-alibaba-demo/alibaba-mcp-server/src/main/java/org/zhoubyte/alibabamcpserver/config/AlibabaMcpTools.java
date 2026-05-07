package org.zhoubyte.alibabamcpserver.config;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AlibabaMcpTools {

    @McpTool(name = "getTimeByZone", description = "根据传入的时区获取当前时间")
    public String getTimeByZone(@McpToolParam(description = "时区，比如 Asia/Shanghai") String zone) {
        ZoneId zoneId = ZoneId.of(zone);
        ZonedDateTime zoneddateTime = ZonedDateTime.now(zoneId);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zoneddateTime);
    }

    @Tool(name = "getCurrentUser", description = "获取当前登录用户的用户名称")
    public String getCurrentUser(){
        return "ZhouByte";
    }
}
