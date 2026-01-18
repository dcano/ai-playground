package io.twba.aiplayground;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfiguration {

    @ConditionalOnProperty(prefix = "playground.model", name = "type", havingValue = "ollama")
    @Bean("chatClientBuilder")
    public ChatClient.Builder chatClientBuilderOllama(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .defaultOptions(ChatOptions.builder()
                        .model("gpt-oss:latest")
                        .temperature(0.8)
                        .maxTokens(10000)
                        .build());
    }


    @ConditionalOnProperty(prefix = "playground.model", name = "type", havingValue = "google-genai")
    @Bean("chatClientBuilder")
    public ChatClient.Builder chatClientBuilderGoogleGenAi(GoogleGenAiChatModel googleGenAiChatModel) {
        return ChatClient.builder(googleGenAiChatModel);
    }

    @Bean("evaluatorChatClientBuilder")
    public ChatClient.Builder evaluatorChatClientBuilderOllama(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .defaultOptions(ChatOptions.builder()
                        .model("gpt-oss:latest")
                        .build());
    }

}
