package com.zhoubyte.spring_ai_alibaba_demo.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorChatClientConfig {

    @Bean("ragChatClient")
    public ChatClient ragChatClient(ChatModel chatModel, VectorStore vectorStore) {
        VectorStoreDocumentRetriever retrieverBuild = VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).topK(3).similarityThreshold(0.5).build();
        RetrievalAugmentationAdvisor augmentationAdvisor = RetrievalAugmentationAdvisor.builder().documentRetriever(retrieverBuild).order(0).build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(augmentationAdvisor)
                .build();
    }
}
