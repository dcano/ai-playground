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
public class OllamaAiModelConfiguration {

    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory
                .builder()
                .maxMessages(10)
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();
    }

    @Bean
    public ChatClient plainChatClient(OllamaChatModel ollamaChatModel,
                                      ChatOptions chatOptions,
                                      PlaygroundProperties playgroundProperties,
                                      ChatMemory chatMemory,
                                      RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {

        ChatClient.Builder builder = ChatClient.builder(ollamaChatModel)
                .defaultOptions(chatOptions);

        if(playgroundProperties.isHasMemory()) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }

        builder.defaultAdvisors(List.of(new SimpleLoggerAdvisor(), new TokenUsageAuditAdvisor(), retrievalAugmentationAdvisor));

        return builder.build();

    }

    @Bean
    public ChatClient ollamaChatClientSoftwareArchitect(OllamaChatModel ollamaChatModel,
                                                        ChatOptions chatOptions,
                                                        PlaygroundProperties playgroundProperties,
                                                        ChatMemory chatMemory) {

        ChatClient.Builder builder = ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(), new TokenUsageAuditAdvisor())
                .defaultOptions(chatOptions)
                .defaultSystem("""
                        You are a software architect expert on healthcare sector, your role is to help other colleagues in the\s
                        organization on specific technical topics, focusing on healthcare system integrations, healthcare privacy\s
                        and regulatory policies, etc. You also provide support on identifying key quality attributes applicable to\s
                        specific product requirements providing architecture patterns recommendations, considering the pros and cons\s
                        of the recommended solutions. If the user asks for help with anything outside these topics, kindly inform\s
                        them that you can only assist with queries related to software architecture.\s
                        """);

        if(playgroundProperties.isHasMemory()) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }

        return builder
                .build();
    }

    @Bean
    public ChatClient ollamaChatClientPlain(OllamaChatModel ollamaChatModel,
                                            ChatOptions chatOptions,
                                            PlaygroundProperties playgroundProperties,
                                            ChatMemory chatMemory) {

        ChatClient.Builder builder = ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(chatOptions);

        if(playgroundProperties.isHasMemory()) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }

        return builder.build();
    }

    @Bean
    public ChatOptions chatOptions() {
        return ChatOptions.builder()
                .model("gpt-oss:latest")
                .temperature(0.8)
                .maxTokens(10000)
                .build();
    }

    @Bean
    public ChatClient.Builder chatClientBuilder(OllamaChatModel ollamaChatModel,
                                                ChatMemory chatMemory,
                                                ChatOptions chatOptions) {

        return ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        new TokenUsageAuditAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build())
                .defaultOptions(chatOptions);
    }

    @ConfigurationProperties(prefix = "playground")
    @Bean
    public PlaygroundProperties playgroundProperties() {
        return new PlaygroundProperties();
    }

    @Bean
    public ChatClient webSearchRAGChatClient(OllamaChatModel ollamaChatModel,
                                             ChatOptions chatOptions,
                                             ChatMemory chatMemory,
                                             RestClient.Builder restClientBuilder,
                                             PlaygroundProperties playgroundProperties) {
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
        return ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(loggerAdvisor, tokenUsageAdvisor, memoryAdvisor, webSearchRAGAdvisor)
                .defaultOptions(chatOptions)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "playground.manual-error-handling", name="enabled",  havingValue = "true")
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new DefaultToolExecutionExceptionProcessor(true);
    }
}
