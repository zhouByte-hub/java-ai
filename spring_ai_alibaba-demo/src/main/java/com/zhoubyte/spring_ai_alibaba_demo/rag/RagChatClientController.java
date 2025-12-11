package com.zhoubyte.spring_ai_alibaba_demo.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping(value = "/rag")
public class RagChatClientController {

    private final ChatClient ragChatClient;
    private final PgVectorStore pgVectorStore;

    public RagChatClientController(ChatClient ragChatClient,  PgVectorStore pgVectorStore) {
        this.ragChatClient = ragChatClient;
        this.pgVectorStore = pgVectorStore;
    }

    @GetMapping(value = "/chat")
    public Flux<String> chat(@RequestParam(value = "message") String message) {
        return ragChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }


    @GetMapping(value = "/embedding")
    public void embeddingContent(@RequestParam("message") String message) {
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(50)
                .withKeepSeparator(true)
                .withMaxNumChunks(1024)
                .withMinChunkLengthToEmbed(20)
                .withMinChunkSizeChars(10)
                .build();
        List<Document> documentList = tokenTextSplitter.split(Document.builder().text(message).build());
        pgVectorStore.add(documentList);
    }

}
