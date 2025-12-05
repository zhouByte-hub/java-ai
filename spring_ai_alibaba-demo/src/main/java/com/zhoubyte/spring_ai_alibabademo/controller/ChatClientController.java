package com.zhoubyte.spring_ai_alibabademo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/chatClient")
@RequiredArgsConstructor
public class ChatClientController {

    private final ChatClient chatClient;

    @GetMapping(value = "/chat")
    public Flux<String> stream(@RequestParam("message") String message) {
        return chatClient.prompt(new Prompt(message)).stream().content();
    }
}
