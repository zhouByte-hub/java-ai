package com.zhoubyte.spring_ai_demo.adviser;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

@Component
public class SensitiveWordAdviser implements CallAdvisor, StreamAdvisor {

    private final static String[] SENSITIVE_WORDS = {"赌博", "嫖娼", "吸毒"};

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        context.forEach((key, value) -> {
            if(checkSensitiveWord(String.valueOf(value))) {
                throw new RuntimeException("存在敏感词，注意注意!!!");
            }
        });
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        chatClientResponse.context().put("server", "springAi");
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        context.forEach((key, value) -> {
            if(checkSensitiveWord(String.valueOf(value))) {
                throw new RuntimeException("存在敏感词，注意注意!!!");
            }
        });
        // 对流式响应逐条添加自定义上下文属性
        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnNext(resp -> resp.context().put("server", "springAi"));
    }

    @Override
    public String getName() {
        return "SensitiveWordAdviser";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private boolean checkSensitiveWord(String content) {
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if(content.contains(sensitiveWord)) {
                return true;
            }
        }
        return false;
    }
}
