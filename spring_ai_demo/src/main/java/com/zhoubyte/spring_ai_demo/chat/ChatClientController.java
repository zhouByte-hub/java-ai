package com.zhoubyte.spring_ai_demo.chat;

import com.zhoubyte.spring_ai_demo.adviser.SensitiveWordAdviser;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/chatClient")
@RequiredArgsConstructor
public class ChatClientController {

    private final ChatClient ollamaChatClient;
    private final SensitiveWordAdviser sensitiveWordAdviser;

    /**
     * 使用 ChatClient实现基本对话
     * @param message 用户信息
     * @return 大模型返回数据
     */
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
        return ollamaChatClient
                .prompt(new Prompt(message))
                .advisors(sensitiveWordAdviser)
                .stream()
                .content();
    }


    @GetMapping(value = "/chatForMemories")
    public Flux<String> chatForMemories(@RequestParam("message") String message, @RequestParam("sessionId") String sessionId) {
        return ollamaChatClient.prompt(message)
                .advisors(advisorSpec -> advisorSpec.param("chat_memories_session_id", sessionId))
                .stream()
                .content();
    }
}
