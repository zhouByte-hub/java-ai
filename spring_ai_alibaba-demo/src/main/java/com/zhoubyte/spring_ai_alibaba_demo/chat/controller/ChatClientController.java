package com.zhoubyte.spring_ai_alibaba_demo.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping(value = "/chatClient")
public class ChatClientController {

    private final ChatClient ollamaChatClient;

    public ChatClientController(ChatClient ollamaChatClient) {
        this.ollamaChatClient = ollamaChatClient;
    }

    @GetMapping(value = "/chat")
    public Flux<String> stream(@RequestParam("message") String message) {
        return ollamaChatClient.prompt(new Prompt(message)).stream().content();
    }


    @GetMapping(value = "/prompt")
    public Flux<String> prompt(){
        // 系统提示词
//        SystemPromptTemplate systemPromptTemplate = SystemPromptTemplate.builder().template("").variables(Map.of()).build();

        // 用户提示词
        PromptTemplate build = PromptTemplate.builder().template("").variables(Map.of()).build();
        Prompt prompt = build.create(Map.of());
        return ollamaChatClient.prompt(prompt).stream().content();
    }
}
