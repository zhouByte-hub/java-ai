package com.zhoubyte.spring_ai_demo.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping(value = "/rag")
@RequiredArgsConstructor
public class RagController {

    private final VectorStore pgVectorStore;
    private final ChatClient ragChatClient;

    /**
     * 添加文档
     * @param content 需要添加的文档
     */
    @GetMapping(value = "/import_text")
    public void importTextData(@RequestParam("content") String content) {
        Document build = Document.builder().text(content).build();
        pgVectorStore.add(List.of(build));
    }

    /**
     * 相似性搜索
     * @param query 搜索内容
     * @return 搜索内容
     */
    @GetMapping(value = "/query")
    public Flux<Document> search(@RequestParam("query") String query) {
        SearchRequest build = SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.5)
                .build();
        List<Document> documents = pgVectorStore.similaritySearch(build);
        return Flux.fromIterable(documents);
    }

    /**
     * 执行 Rag 操作
     * @param message 用户提问
     * @return 响应结果
     */
    @GetMapping(value = "/message")
    public Flux<String> message(@RequestParam("message") String message)  {
        return ragChatClient.prompt()
                .system("这里是 springAi 项目，有什么能够帮您？")
                .user(message)
                .stream()
                .content();
    }

}
