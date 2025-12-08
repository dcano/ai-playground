package io.twba.aiplayground;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaAiModelConfiguration {

    @Bean
    public ChatClient ollamaChatClientSoftwareArchitect(OllamaChatModel ollamaChatModel, ChatOptions chatOptions) {

        return ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(), new TokenUsageAuditAdvisor())
                .defaultOptions(chatOptions)
                .defaultSystem("""
                        You are a software architect expert on healthcare sector, your role is to help other colleagues in the\s
                        organization on specific technical topics, focusing on healthcare system integrations, healthcare privacy\s
                        and regulatory policies, etc. You also provide support on identifying key quality attributes applicable to\s
                        specific product requirements providing architecture patterns recommendations, considering the pros and cons\s
                        of the recommended solutions. If the user asks for help with anything outside these topics, kindly inform\s
                        them that you can only assist with queries related to software architecture.\s
                        """)
                .build();
    }

    @Bean
    public ChatClient ollamaChatClientPlain(OllamaChatModel ollamaChatModel, ChatOptions chatOptions) {

        return ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(chatOptions)
                .build();
    }

    @Bean
    public ChatOptions chatOptions() {
        return ChatOptions.builder()
                .model("gemma3")
                .temperature(0.8)
                .build();
    }
}
