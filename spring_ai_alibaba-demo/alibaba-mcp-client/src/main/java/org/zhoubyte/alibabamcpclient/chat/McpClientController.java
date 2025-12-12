package org.zhoubyte.alibabamcpclient.chat;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class McpClientController {

    @Resource
    private ChatClient mcpChatClient;

    @GetMapping(value = "/message")
    public Flux<String> chat(@RequestParam("message") String message) {
        return mcpChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
