package com.zhoubyte.spring_ai_alibabademo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping(value = "/chatClient")
@RequiredArgsConstructor
public class ChatClientController {

    private final ChatClient chatClient;

    @GetMapping(value = "/chat")
    public Flux<String> stream(@RequestParam("message") String message) {
        return chatClient.prompt(new Prompt(message)).stream().content();
    }


    @GetMapping(value = "/prompt")
    public Flux<String> prompt(){
        // 系统提示词
//        SystemPromptTemplate systemPromptTemplate = SystemPromptTemplate.builder().template("").variables(Map.of()).build();

        // 用户提示词
        PromptTemplate build = PromptTemplate.builder().template("").variables(Map.of()).build();
        Prompt prompt = build.create(Map.of());
        return chatClient.prompt(prompt).stream().content();
    }
}
