package io.twba.aiplayground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    @Value("classpath:/promptTemplates/userPromptTemplate.st")
    private Resource promptTemplate;

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    public ChatController(@Qualifier("chatClientPlain") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/chat-help-desk")
    public String chatHelpDesk(@RequestParam("message") String message) {
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
                .call()
                .content();
    }

    @GetMapping("/email")
    public String emaiLResponse(@RequestParam("customerName") String customerName,
                                @RequestParam("customerMessage") String customerMessage) {

        return chatClient
                .prompt()
                .system("""
                        You are a professional customer service assistant which helps drafting email\s
                        responses to improve the productivity of the customer support team
                        """)
                .user(promptTemplateSpec -> promptTemplateSpec.text(promptTemplate)
                        .param("customerName", customerName)
                        .param("customerMessage", customerMessage))
                .call().content();

    }

    @GetMapping("/prompt-stuffing")
    public String promptStuffing(@RequestParam("message") String message) {

        return chatClient
                .prompt()
                .system(systemPromptTemplate)
                .user(promptTemplateSpec -> promptTemplateSpec.text(message))
                .call().content();

    }

}
