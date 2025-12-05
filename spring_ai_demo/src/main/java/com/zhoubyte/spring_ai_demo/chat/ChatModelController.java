package com.zhoubyte.spring_ai_demo.chat;

import com.zhoubyte.spring_ai_demo.adviser.SensitiveWordAdviser;
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

    // chatModel无法设置 Adviser
    private final SensitiveWordAdviser sensitiveWordAdviser;

    @GetMapping(value = "/chat")
    public Flux<String> chat(@RequestParam("message") String message) {

        /* 动态设置配置
         * OllamaChatOptions options = OllamaChatOptions.builder()
         *     .model("设置模型")
         *     .temperature(0.8)
         *     .enableThinking()
         *     .topK(10)
         *     .build();
         *  Prompt prompt = new Prompt(message, options);
         */
        return chatModel.stream(new Prompt(message))
                .map(ChatResponse::getResult)
                .mapNotNull(item -> item.getOutput().getText());
    }
}
