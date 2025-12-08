package com.zhoubyte.spring_ai_demo.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ChatClient toolChatClient;

    @GetMapping(value = "/message")
    public Flux<String> chat(@RequestParam("message") String message) {
        return toolChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

}
