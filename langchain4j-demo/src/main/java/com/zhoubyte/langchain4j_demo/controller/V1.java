package com.zhoubyte.langchain4j_demo.controller;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.language.LanguageModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/** LangChain4j提供了两类低级的 API
 *      - LanguageModel。它们的 API 非常简单 - 接受 String 作为输入并返回 String 作为输出。 这种 API 现在正在被聊天 API（第二种 API 类型）所取代。
 *      - ChatLanguageModel。这些接受多个 ChatMessage 作为输入并返回单个 AiMessage 作为输出。 ChatMessage 通常包含文本，但某些 LLM 也支持其他模态（例如，图像、音频等）。
 */
@RestController
@RequestMapping(value = "/v1")
public class V1 {

    private final ChatModel chatModel;

    public V1(ChatModel ollamaChatModel) {
        this.chatModel = ollamaChatModel;
    }

    @GetMapping(value = "/chat_base")
    public String chat(@RequestParam("message") String message) {
        return chatModel.chat(message);
    }

    @GetMapping(value = "/chat_advance")
    public ChatResponse chatAdvance(@RequestParam("message") String message) {
        UserMessage userMessage = UserMessage.builder().addContent(TextContent.from(message)).build();
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .build();
        return chatModel.chat(chatRequest);
    }


}
