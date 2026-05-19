package com.zhoubyte.spring_ai_alibaba_demo.chat.controller;

import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ChatClient是高级 API
 */
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

    @GetMapping(value = "/options")
    public Object options(@RequestParam("message") String message) {
        ChatOptions build = ChatOptions.builder().temperature(0.8).topK(10).model("qwen 2.5").build();
        return ollamaChatClient.prompt(Prompt.builder().chatOptions(build).build()).user(message).call();
    }

    @GetMapping(value = "/toEntity")
    public Book toEntity(@RequestParam("message") String message) {
        return ollamaChatClient.prompt().user(message).call().entity(Book.class);
    }


    @Data
    public static class Book {
        private String bookId;
        private String bookName;
        private String author;
        private LocalDateTime publishTime;
        private String description;
        private String subTitle;
    }
}
