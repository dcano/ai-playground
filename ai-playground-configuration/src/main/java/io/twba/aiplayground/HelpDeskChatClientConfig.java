package io.twba.aiplayground;

import io.twba.aiplayground.tools.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class HelpDeskChatClientConfig {

    @Value("classpath:/promptTemplates/helpDeskSystemPromptTemplate.st")
    Resource systemPromptTemplate;

    @Bean("helpDeskChatClient")
    public ChatClient chatClient(@Qualifier("chatClientBuilderNonMcp") ChatClient.Builder chatClientBuilder, TimeTools timeTools) {
        return chatClientBuilder.clone()
                .defaultSystem(systemPromptTemplate)
                .defaultAdvisors(ToolCallAdvisor.builder().build())
                .defaultTools(timeTools)
                .build();
    }

}
