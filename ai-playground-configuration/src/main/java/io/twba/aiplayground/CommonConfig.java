package io.twba.aiplayground;

import io.twba.aiplayground.rag.WebSearchDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
@EnableConfigurationProperties
public class CommonConfig {

    @Bean("chatClientBuilderNonMcp")
    public ChatClient.Builder chatClientBuilderNonMcp(ChatClient.Builder chatClientBuilder,
                                                      ChatMemory chatMemory) {

        return chatClientBuilder.clone()
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        new TokenUsageAuditAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build());
    }

    @Bean("chatClientBuilderMcp")
    public ChatClient.Builder chatClientBuilderMcp(ChatClient.Builder chatClientBuilder) {

        return chatClientBuilder.clone()
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        new TokenUsageAuditAdvisor());
    }

    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory
                .builder()
                .maxMessages(10)
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();
    }

    @Bean
    //public ChatClient plainChatClient(ChatClient.Builder chatClientBuilder,
    public ChatClient ragChatClient(ChatClient.Builder chatClientBuilder,
                                      PlaygroundProperties playgroundProperties,
                                      ChatMemory chatMemory,
                                      RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {

        ChatClient.Builder chatClientBuilderClone = chatClientBuilder.clone();

        if(playgroundProperties.isHasMemory()) {
            chatClientBuilderClone.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }

        chatClientBuilderClone.defaultAdvisors(List.of(
                new SimpleLoggerAdvisor(),
                new TokenUsageAuditAdvisor(),
                retrievalAugmentationAdvisor));

        return chatClientBuilderClone.build();

    }

    @Bean
    public ChatClient chatClientSoftwareArchitect(ChatClient.Builder chatClientBuilder,
                                                  PlaygroundProperties playgroundProperties,
                                                  ChatMemory chatMemory) {

        ChatClient.Builder chatClientBuilderClone = chatClientBuilder.clone();

        chatClientBuilderClone
                .defaultAdvisors(new SimpleLoggerAdvisor(), new TokenUsageAuditAdvisor())
                .defaultSystem("""
                        You are a software architect expert on healthcare sector, your role is to help other colleagues in the\s
                        organization on specific technical topics, focusing on healthcare system integrations, healthcare privacy\s
                        and regulatory policies, etc. You also provide support on identifying key quality attributes applicable to\s
                        specific product requirements providing architecture patterns recommendations, considering the pros and cons\s
                        of the recommended solutions. If the user asks for help with anything outside these topics, kindly inform\s
                        them that you can only assist with queries related to software architecture.\s
                        """);

        if(playgroundProperties.isHasMemory()) {
            chatClientBuilderClone.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }

        return chatClientBuilderClone
                .build();
    }

    @Bean
    public ChatClient chatClientPlain(ChatClient.Builder chatClientBuilder,
                                      PlaygroundProperties playgroundProperties,
                                      ChatMemory chatMemory) {

        ChatClient.Builder builder = chatClientBuilder.clone();
        builder.defaultAdvisors(new SimpleLoggerAdvisor());
        if(playgroundProperties.isHasMemory()) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        return builder.build();
    }

    @Bean
    public ChatClient webSearchRAGChatClient(ChatClient.Builder chatClientBuilder,
                                             ChatMemory chatMemory,
                                             RestClient.Builder restClientBuilder,
                                             PlaygroundProperties playgroundProperties) {

        ChatClient.Builder builder = chatClientBuilder.clone();
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAuditAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        var webSearchRAGAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(WebSearchDocumentRetriever.builder()
                        .restClientBuilder(restClientBuilder)
                        .apiKey(playgroundProperties.getTavilyApiKey())
                        .maxResults(playgroundProperties.getTavilyResultLimit())
                        .build())
                .build();
        return builder
                .defaultAdvisors(loggerAdvisor, tokenUsageAdvisor, memoryAdvisor, webSearchRAGAdvisor)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "playground.error-handling", name="type",  havingValue = "manual")
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new DefaultToolExecutionExceptionProcessor(true);
    }

    @ConfigurationProperties(prefix = "playground")
    @Bean
    public PlaygroundProperties playgroundProperties() {
        return new PlaygroundProperties();
    }

}
