package io.twba.aiplayground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Consumer;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/api")
public class ChatMemoryController {

    private final ChatClient chatClient;

    public ChatMemoryController(@Qualifier("plainChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat-memory")
    public ResponseEntity<String> chatMemory(
            @RequestHeader("X-Chat-Session-Id") final String chatSessionId,
            @RequestParam("message") String message) {
        return ResponseEntity.ok(
                chatClient
                .prompt()
                .user(message)
                        .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, chatSessionId))
                .call()
                .content());
    }
}
