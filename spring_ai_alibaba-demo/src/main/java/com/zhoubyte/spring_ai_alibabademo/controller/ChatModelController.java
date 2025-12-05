package com.zhoubyte.spring_ai_alibabademo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/chatModel")
@RequiredArgsConstructor
public class ChatModelController {

    private final ChatModel chatModel;

    @GetMapping(value = "/chat")
    public Flux<String> chat(@RequestParam("message") String message){
        return chatModel.stream(new Prompt(message)).map(ChatResponse::getResult).mapNotNull(item -> item.getOutput().getText());
    }
}
