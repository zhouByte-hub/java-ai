package org.zhoubyte.mcpserver.stdio;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ZoomTool {

    @Tool(description = "通过时区 ID 获取当前时间")
    public String getTimeByZone(@ToolParam(description = "时区 ID，比如 Asia/Shanghai") String zone) {
        ZoneId zoneId = ZoneId.of(zone);
        ZonedDateTime zoneddateTime = ZonedDateTime.now(zoneId);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zoneddateTime);
    }
}
