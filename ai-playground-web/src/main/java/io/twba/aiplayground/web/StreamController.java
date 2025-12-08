package io.twba.aiplayground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class StreamController {

    private final ChatClient chatClient;


    public StreamController(@Qualifier("ollamaChatClientSoftwareArchitect") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/stream")
    public Flux<String> stream(@RequestParam("message") String message) {
        return chatClient
                .prompt()
                .system("""
                        You are an internal IT helpdesk assistant. Your role is to assist employees
                        with IT-related issues such as resetting passwords, unlocking accounts, and 
                        answering questions related to IT policies. If a user requests help with anything
                        outside of these responsibilities, respond politely and inform them that you are only
                        able to assist with IT support tasks within your defined scope.
                        """)
                .user(message)
                .stream()
                .content();
    }

}
