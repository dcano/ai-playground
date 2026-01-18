package io.twba.aiplayground.web;

import io.twba.aiplayground.InvalidAnswerException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    @Value("classpath:/promptTemplates/userPromptTemplate.st")
    private Resource promptTemplate;

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    private final FactCheckingEvaluator factCheckingEvaluator;

    public ChatController(@Qualifier("chatClientPlain") ChatClient chatClient, @Qualifier("evaluatorChatClientBuilder") ChatClient.Builder evaluatorChatClientBuilder) {
        this.chatClient = chatClient;
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(evaluatorChatClientBuilder).build();
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

    @Retryable(retryFor = InvalidAnswerException.class, maxAttempts = 2)
    @GetMapping("/evaluate/chat")
    public String chatWithEvaluation(@RequestParam("message") String message) {
        String response = chatClient
                .prompt()
                .user(message)
                .call()
                .content();

        validateAnswer(message, response);

        return response;
    }

    @Retryable(retryFor = InvalidAnswerException.class, maxAttempts = 2)
    @GetMapping("/evaluate/prompt-stuffing")
    public String promptStuffingWithEvaluation(@RequestParam("message") String message) throws IOException {

        String response = chatClient
                .prompt()
                .system(systemPromptTemplate)
                .user(promptTemplateSpec -> promptTemplateSpec.text(message))
                .call().content();

        validateAnswer(message, response, systemPromptTemplate.getContentAsString(StandardCharsets.UTF_8));

        return response;
    }

    private void validateAnswer(String message, String answer) {
        validateAnswer(message, answer, "");
    }

    private void validateAnswer(String message, String answer, String context) {
        EvaluationRequest evaluationRequest = new EvaluationRequest(message, List.of(new Document(context)), answer);
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);
        if(!evaluationResponse.isPass()) {
            throw new InvalidAnswerException(message, answer);
        }
    }

    @Recover
    public String fallbackFor(InvalidAnswerException invalidAnswerException) {
        return "I'm sorry, I couldn't answer exception, please try to rephrase it.";
    }
}
