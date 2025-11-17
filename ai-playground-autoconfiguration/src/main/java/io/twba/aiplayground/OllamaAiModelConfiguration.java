package io.twba.aiplayground;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaAiModelConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public ChatClient ollamaChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

}
