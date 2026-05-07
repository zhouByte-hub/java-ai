package com.zhoubyte.spring_ai_demo.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
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

        // TokenTextSplitter 的核心作用：按 token 粒度把长文本切成适合做向量检索的小块。
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
                50,   // chunkSize：每块目标最大 token 数，越大每块越长，上下文越长也越慢
                20,            // minChunkSizeChars：太短的块会和前后合并，避免很多碎片
                10,             // minChunkLengthToEmbed：短于这个 token 数的不做向量化，过滤无信息块
                1000,           // maxNumChunks：单个文档最多切多少块，防止极长文档爆炸
                false           // keepSeparator：是否保留分隔符，对性能影响不大
        );
        pgVectorStore.add(tokenTextSplitter.split(build));
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
     *
     * 步骤：
     *      - 用你的问题做一次 embedding（qwen3-embedding:0.6b，走 Ollama 一次）
     *      - 拿这个向量去 PostgreSQL + pgvector 里查相似文档
     *      - 把查到的文档拼成一个很长的上下文（context）
     *      - 再把「系统提示 + context + 用户问题」一起丢给聊天模型 qwen3-vl:2b（再走 Ollama 一次）
     *
     * 优化方案：能优化的大方向主要有三块：模型速度、上下文大小、检索开销
     *      - 模型速度：模型越小、越“文本专用”，RAG 响应就越快。
     *      - 上下文太长，LLM 推理肯定慢；让最终拼给模型的文本别太长——小一点的 chunkSize、较小的 topK，可以显著减轻 LLM 负担。
     *
     */
    @GetMapping(value = "/message")
    public Flux<String> message(@RequestParam("query") String message)  {
        return ragChatClient.prompt()
                .system("这里是 springAi 项目，有什么能够帮您？")
                .user(message)
                .stream()
                .content();
    }

}
