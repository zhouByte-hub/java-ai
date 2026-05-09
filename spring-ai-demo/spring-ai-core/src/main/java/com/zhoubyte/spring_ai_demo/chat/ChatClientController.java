package com.zhoubyte.spring_ai_demo.chat;

import com.zhoubyte.spring_ai_demo.chat.adviser.SensitiveWordAdviser;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ChatClient高级封装API示例
 * <p>
 * ChatClient是Spring AI对ChatModel的高级封装，提供Fluent API：
 * - 支持链式调用（.prompt()、.advisors()、.stream().content()）
 * - 支持Advisors（敏感词过滤、记忆管理、RAG等）
 * - 直接通过.content()获取纯文本，无需手动解析
 * - 支持系统提示词.system()、工具调用.tools()等便捷方法
 * - 适用于日常对话开发，是推荐使用的API
 *
 * @see org.springframework.ai.chat.client.ChatClient
 */
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

    /**
     * 统计Token用量示例
     * <p>
     * ChatClient同样支持获取token用量统计，使用call()方法（非流式）获取完整响应：
     * - 通过ChatClientResponse.chatResponse()获取底层ChatResponse
     * - 再通过getMetadata().getUsage()获取token用量信息
     * <p>
     * 与ChatModel的区别：
     * - ChatClient: 可同时使用Advisors + 获取元数据，更灵活
     * - ChatModel: 直接操作底层响应，代码稍简洁
     *
     * @param message 用户输入的消息
     * @return 包含回复内容和token用量统计的Map
     */
    @GetMapping(value = "/chatWithUsage")
    public Map<String, Object> chatWithUsage(@RequestParam("message") String message) {
        var response = ollamaChatClient.prompt(new Prompt(message))
                .advisors(sensitiveWordAdviser)
                .call();

        Map<String, Object> result = new HashMap<>();
        result.put("content", Objects.requireNonNull(response.chatResponse()).getResult().getOutput().getText());
        result.put("model", Objects.requireNonNull(response.chatResponse()).getMetadata().getModel());
        result.put("usage", Objects.requireNonNull(response.chatResponse()).getMetadata().getUsage());
        return result;
    }

    /**
     * 将大模型的输出转换为结构化类
     * @param userMessage 用户输入
     * @return 结果
     */
    @GetMapping(value = "/complaints")
    public String complaints(@RequestParam("query") String userMessage) {
        Boolean complaints = ollamaChatClient.prompt(new Prompt(userMessage)).call().entity(Boolean.class);
        if(Boolean.TRUE.equals(complaints)) {
            return "用户提交投诉意见，转人工";
        }
        return "用户提交非投诉意见，请稍候处理";
    }
}
