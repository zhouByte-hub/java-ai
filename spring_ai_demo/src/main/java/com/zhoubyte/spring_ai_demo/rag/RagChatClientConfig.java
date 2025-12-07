package com.zhoubyte.spring_ai_demo.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

	@Configuration
	public class RagChatClientConfig {
	
	    /**
	     * 带 RAG 能力的 ChatClient Bean。
	     *
	     * 内部 RAG 流程（对调用方是透明的）：
	     * 1. 业务代码通过 ragChatClient 发送用户问题。
	     * 2. RetrievalAugmentationAdvisor 会在请求进入 ChatModel 前先触发：
	     *    - 使用用户问题做 Embedding，形成查询向量；
	     *    - 使用 VectorStoreDocumentRetriever 在 pgVectorStore 中按相似度检索 topK（这里是 5 条）文档；
	     * 3. 检索到的文档会被拼装成 context，自动注入到 Prompt 中（如作为系统/上下文消息）；
	     * 4. ChatModel 最终收到的是「用户问题 + context」，生成答案并返回给调用方。
	     *
	     * 也就是说：业务只需要注入并调用 ragChatClient，RAG 检索与上下文增强由该配置自动完成。
	     */
	    @Bean
	    public ChatClient ragChatClient(ChatModel chatModel, VectorStore pgVectorStore) {

	        // 基于 pgVectorStore 的文档检索器：按照向量相似度从向量库中取前 topK 条文档
	        VectorStoreDocumentRetriever storeDocumentRetriever = VectorStoreDocumentRetriever.builder()
	                .vectorStore(pgVectorStore).topK(5)
                    .similarityThreshold(0.8).build();
	        // RAG 顾问：在每次对话前先使用检索器查知识库，并把检索结果注入到模型 Prompt 中
	        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
	                .order(0)
	                .documentRetriever(storeDocumentRetriever).build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build();
    }
}
