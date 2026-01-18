package io.twba.aiplayground;

import io.twba.aiplayground.web.ChatController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.qdrant.QdrantContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.org.springframework.ai=DEBUG"
})
@ActiveProfiles("ollama")
class AiPlaygroundApplicationTests {

    @Container
    static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:latest");

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatController chatController;

    @Autowired
    private ChatModel chatModel;

    private ChatClient chatClient;
    private RelevancyEvaluator relevancyEvaluator;
    private FactCheckingEvaluator factCheckingEvaluator;

    // Minimum acceptable relevancy score
    @Value("${test.relevancy.min-score:0.7}")
    private float minRelevancyScore;

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource hrPolicyTemplate;


    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.vectorstore.qdrant.host", qdrant::getHost);
        registry.add("spring.ai.vectorstore.qdrant.port", () -> qdrant.getMappedPort(6334));
    }

    @BeforeEach
    void setup() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).defaultSystem("Whatever response you provide, please do not include any markup symbol.");
        chatClient = chatClientBuilder.build();
        this.relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClientBuilder).build();
    }

    @Test
    void contextLoads() {
        assertThat(qdrant.isRunning()).isTrue();
        assertThat(vectorStore).isNotNull();
    }

    @Test
    @DisplayName("Should return relevant response for basic geography question")
    @Timeout(value = 30)
    void evaluateChatControllerResponseRelevancy() {
        // Given
        String question = "What is the capital of Spain? Provide answer in a simple sentence without any markup.";

        // When
        String aiResponse = chatController.chat(question);
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, aiResponse);
        EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);

        // Then
        assertAll(
                () -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(evaluationResponse.isPass()).withFailMessage("""
                                ========================================
                                The answer was not considered relevant.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, question, aiResponse).isTrue(),
                () -> assertThat(evaluationResponse.getScore()).withFailMessage("""
                                ========================================
                                The score %.2f is lower than the minimum required %.2f.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, evaluationResponse.getScore(), minRelevancyScore, question, aiResponse)
                        .isGreaterThan(minRelevancyScore)
        );

    }

    @Test
    @DisplayName("Should return factually correct response for gravity related questions")
    @Timeout(value = 30)
    void evaluateFactAccuracyForGravityQuestions() {
        // Given
        String question = "Who discovered the law of universal gravitation";

        // When
        String aiResponse = chatController.chat(question);
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, aiResponse);
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);

        // Then
        assertAll(
                () -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(evaluationResponse.isPass()).withFailMessage("""
                                ========================================
                                The answer was not considered factually correct.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, question, aiResponse).isTrue());
    }

    @Test
    @DisplayName("Should correctly evaluate factual response based on HR policy context (RAG)")
    @Timeout(value = 30)
    void evaluateHrPolicyAnswerWithRagContext() throws IOException {
        // Given
        String question = "How many paid leaves do employees get annually?";

        // When
        String aiResponse = chatController.promptStuffing(question);
        String retrievedContext = hrPolicyTemplate.getContentAsString(StandardCharsets.UTF_8);
        EvaluationRequest evaluationRequest = new EvaluationRequest(
                question,
                List.of(new Document(retrievedContext)),
                aiResponse
        );
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);

        // Then
        assertAll(
                () -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(evaluationResponse.isPass()).withFailMessage("""
                                ========================================
                                The answer was not considered factually correct.
                                Question: "%s"
                                Response: "%s"
                                Context: "%s"
                                ========================================
                                """, question, aiResponse, retrievedContext).isTrue());
    }

}
