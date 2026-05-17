package io.twba.skills;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/developer-expert")
public class DeveloperExpertController {

    private final ChatClient chatClient;

    public DeveloperExpertController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * POST /api/developer-expert/chat
     * Body: plain-text development question
     */
    @PostMapping("/chat")
    public String chat(@RequestBody String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * GET /api/developer-expert/chat?question=...
     * Convenient for quick browser or curl tests.
     */
    @GetMapping("/chat")
    public String chatGet(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
