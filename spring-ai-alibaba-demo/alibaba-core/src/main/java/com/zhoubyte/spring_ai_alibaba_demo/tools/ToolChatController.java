package com.zhoubyte.spring_ai_alibaba_demo.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/tool")
public class ToolChatController {

    private final ChatClient toolChatClient;

    public ToolChatController(ChatClient toolChatClient) {
        this.toolChatClient = toolChatClient;
    }

    @GetMapping(value = "/chat")
    public Flux<String> chat(@RequestParam("message") String message) {
        return toolChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
