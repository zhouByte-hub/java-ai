package com.zhoubyte.spring_ai_demo.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * ChatModel底层API示例
 * <p>
 * ChatModel是Spring AI提供的低级API，直接与AI模型交互：
 * - 返回原始ChatResponse，需要手动解析结果
 * - 不支持Advisors（如敏感词过滤、记忆管理等）
 * - 适用于需要访问完整元数据（token用量、模型信息等）或需要精细控制响应处理的场景
 * - 相比ChatClient更灵活但代码更繁琐
 *
 * @see org.springframework.ai.chat.model.ChatModel
 */
@RestController
@RequestMapping(value = "/chatModel")
@RequiredArgsConstructor
public class ChatModelController {

    private final ChatModel chatModel;

    // chatModel无法设置 Adviser
//    private final SensitiveWordAdviser sensitiveWordAdviser;

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

    /**
     * 统计Token用量示例
     * <p>
     * 使用ChatModel的call()方法（非流式）获取完整响应，
     * 通过ChatResponse.getMetadata().getUsage()获取token用量信息：
     * - promptTokens: 输入提示词消耗的token数
     * - generationTokens: 模型生成内容消耗的token数
     * - totalTokens: 总计消耗的token数
     * <p>
     * 注意：此场景必须使用ChatModel而非ChatClient，因为ChatClient无法直接访问元数据
     *
     * @param message 用户输入的消息
     * @return 包含回复内容和token用量统计的Map
     */
    @GetMapping(value = "/chatWithUsage")
    public Map<String, Object> chatWithUsage(@RequestParam("message") String message) {
        ChatResponse response = chatModel.call(new Prompt(message));

        Map<String, Object> result = new HashMap<>();
        result.put("content", response.getResult().getOutput().getText());
        result.put("model", response.getMetadata().getModel());
        result.put("usage", response.getMetadata().getUsage());
        return result;
    }
}
