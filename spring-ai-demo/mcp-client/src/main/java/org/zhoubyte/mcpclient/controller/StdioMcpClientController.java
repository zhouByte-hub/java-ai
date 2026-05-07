package org.zhoubyte.mcpclient.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/stdio")
public class StdioMcpClientController {

    private final ChatClient stdioMcpChatClient;

    public StdioMcpClientController(ChatClient stdioMcpChatClient) {
        this.stdioMcpChatClient = stdioMcpChatClient;
    }

    @GetMapping(value = "/chat")
    public Flux<String> chat(@RequestParam("message") String message) {
        return stdioMcpChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
