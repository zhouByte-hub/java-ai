package com.zhoubyte.spring_ai_alibaba_demo.chat.adviser;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简单的对话记忆实现 - 基于 ChatMemory 接口
 * 
 * <h3>ChatMemory 接口作用</h3>
 * <p><b>职责</b>：管理对话记忆的<b>策略</b>（决定保留哪些消息、何时删除）</p>
 * 
 * <h4>核心方法：</h4>
 * <ul>
 *   <li><b>add()</b>：添加对话消息到记忆中</li>
 *   <li><b>get()</b>：获取指定会话的对话历史</li>
 *   <li><b>clear()</b>：清除指定会话的记忆</li>
 * </ul>
 * 
 * <h4>典型实现：</h4>
 * <ul>
 *   <li><b>MessageWindowChatMemory</b>：固定大小消息窗口（默认20条）</li>
 *   <li><b>SimpleMemories</b>（本类）：简单内存缓存，无数量限制</li>
 * </ul>
 * 
 * <h3>ChatMemory vs ChatMemoryRepository 区别</h3>
 * <table border="1">
 *   <tr><th>接口</th><th>职责</th><th>关注点</th></tr>
 *   <tr><td>ChatMemory</td><td>管理策略</td><td>怎么管理（保留多少、何时删除）</td></tr>
 *   <tr><td>ChatMemoryRepository</td><td>存储实现</td><td>存在哪里（内存、数据库）</td></tr>
 * </table>
 *
 * @see org.springframework.ai.chat.memory.MessageWindowChatMemory
 */
@Component
public class SimpleMemories implements ChatMemory {

    /**
     * 对话记忆缓存
     * Key: conversationId（会话ID）
     * Value: 该会话的消息列表
     */
    private final static Map<String, List<Message>> MEMORIES_CACHE = new HashMap<>();

    /**
     * 添加消息到指定会话
     * @param conversationId 会话ID
     * @param messages 要添加的消息列表
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> memories = MEMORIES_CACHE.get(conversationId);
        if (memories == null) {
            memories = new ArrayList<>();
        }
        if(!messages.isEmpty()){
            memories.addAll(messages);
        }
        MEMORIES_CACHE.put(conversationId, memories);
    }

    /**
     * 获取指定会话的消息列表
     * @param conversationId 会话ID
     * @return 消息列表，不存在则返回空列表
     */
    @Override
    public List<Message> get(String conversationId) {
        List<Message> messages = MEMORIES_CACHE.get(conversationId);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    /**
     * 清除指定会话的记忆
     * @param conversationId 会话ID
     */
    @Override
    public void clear(String conversationId) {
        List<Message> messages = MEMORIES_CACHE.get(conversationId);
        if (messages != null) {
            messages.clear();
        }
    }
}
