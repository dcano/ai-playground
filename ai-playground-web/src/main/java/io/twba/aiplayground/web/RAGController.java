package io.twba.aiplayground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private final ChatClient chatClient;
    private final ChatClient webSearchRagChatClient;
    private final VectorStore vectorStore;

    @Value("classpath:/promptTemplates/systemPromptRandomDataTemplate.st")
    private Resource promptTemplate;

    @Value("classpath:/promptTemplates/systemPromptHrAssistantRagTemplate.st")
    private Resource hrSystemPromptTemplate;


    @Autowired
    public RAGController(@Qualifier("plainChatClient") ChatClient chatClient,
                         VectorStore vectorStore,
                         @Qualifier("webSearchRAGChatClient") ChatClient webSearchRagChatClient) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.webSearchRagChatClient = webSearchRagChatClient;
    }

    @GetMapping("/random/chat")
    public ResponseEntity<String> randomChat(
            @RequestHeader("X-Chat-Session-Id") final String chatSessionId,
            @RequestParam("message") String message) {

        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(10)
                .similarityThreshold(0.5)
                .build();

        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
        String similarContext = similarDocs.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));

        String answer = chatClient.prompt().system(
                promptSystemSpec -> promptSystemSpec.text(promptTemplate).param("documents", similarContext))
                .advisors(a -> a.param(CONVERSATION_ID, chatSessionId))
                .user(message)
                .call().content();

        return ResponseEntity.ok(answer);
    }

    @GetMapping("/document/chat")
    public ResponseEntity<String> documentChat(
            @RequestHeader("X-Chat-Session-Id") final String chatSessionId,
            @RequestParam("message") String message) {

        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(3)
                .similarityThreshold(0.5)
                .build();

        List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
        String similarContext = similarDocs.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));

        String answer = chatClient.prompt().system(
                        promptSystemSpec -> promptSystemSpec.text(hrSystemPromptTemplate).param("documents", similarContext))
                .advisors(a -> a.param(CONVERSATION_ID, chatSessionId))
                .user(message)
                .call().content();

        return ResponseEntity.ok(answer);
    }

    @GetMapping("/managed/chat")
    public ResponseEntity<String> ragManagedChat(
            @RequestHeader("X-Chat-Session-Id") final String chatSessionId,
            @RequestParam("message") String message) {


        String answer = chatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, chatSessionId))
                .user(message)
                .call().content();

        return ResponseEntity.ok(answer);
    }

    @GetMapping("/web-search/chat")
    public ResponseEntity<String> ragWebSearch(
            @RequestHeader("X-Chat-Session-Id") final String chatSessionId,
            @RequestParam("message") String message) {


        String answer = webSearchRagChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, chatSessionId))
                .user(message)
                .call().content();

        return ResponseEntity.ok(answer);
    }
}
