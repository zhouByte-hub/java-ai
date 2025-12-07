package com.zhoubyte.spring_ai_demo.adviser;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MemoriesAdviser implements BaseAdvisor {

    private final static Map<String, List<Message>> MEMORIES = new HashMap<>();
    private final static String CHAT_MEMORIES_SESSION_ID = "chat_memories_session_id";

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String chatMemoriesSessionId = chatClientRequest.context().get(CHAT_MEMORIES_SESSION_ID).toString();
        List<Message> messages = MEMORIES.get(chatMemoriesSessionId);
        if(messages == null) {
            messages = new ArrayList<>();
        }
        // 将当前对话和历史对话放在同一个集合中
        messages.addAll(chatClientRequest.prompt().getInstructions());
        Prompt build = chatClientRequest.prompt().mutate()
                .messages(messages)
                .build();
        MEMORIES.put(chatMemoriesSessionId, messages);
        return chatClientRequest.mutate().prompt(build).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        assert chatClientResponse.chatResponse() != null;
        AssistantMessage output = chatClientResponse.chatResponse().getResult().getOutput();
        String chatMemoriesSessionId = chatClientResponse.context().get(CHAT_MEMORIES_SESSION_ID).toString();
        List<Message> messages = MEMORIES.get(chatMemoriesSessionId);
        if(messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(output);
        MEMORIES.put(chatMemoriesSessionId, messages);
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
