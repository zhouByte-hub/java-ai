package com.zhoubyte.spring_ai_alibaba_demo.chat.adviser;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimpleMemories implements ChatMemory {

    private final static Map<String, List<Message>> MEMORIES_CACHE = new HashMap<>();

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> memories = MEMORIES_CACHE.get(conversationId);
        if (memories == null) {
            memories = new ArrayList<>();
        }
        if(messages != null && !messages.isEmpty()){
            memories.addAll(messages);
        }
        MEMORIES_CACHE.put(conversationId, memories);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Message> messages = MEMORIES_CACHE.get(conversationId);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        List<Message> messages = MEMORIES_CACHE.get(conversationId);
        if (messages != null) {
            messages.clear();
        }
    }
}
