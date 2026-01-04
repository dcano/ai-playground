package io.twba.aiplayground;

import io.twba.aiplayground.tools.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeChatClientConfig {

    @Bean("timeChatClient")
    public ChatClient chatClient(@Qualifier("chatClientBuilderNonMcp") ChatClient.Builder chatClientBuilder, TimeTools timeTools) {
        return chatClientBuilder.clone()
                .defaultAdvisors(ToolCallAdvisor.builder().build())
                .defaultTools(timeTools)
                .build();
    }

}
