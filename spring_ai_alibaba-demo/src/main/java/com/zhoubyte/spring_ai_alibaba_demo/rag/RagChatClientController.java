package com.zhoubyte.spring_ai_alibaba_demo.rag;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/rag")
public class RagChatClientController {

    @Qualifier("ragChatClient")
    @Resource
    private ChatClient ragChatClient;

    @GetMapping(value = "/chat")
    public Flux<String> chat(@RequestParam(value = "message") String message) {
        return ragChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

}
