package io.twba.aiplayground.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfiguration {

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(@Autowired VectorStore vectorStore,
                                                                     @Autowired ChatClient.Builder chatClientBuilder) {
        return RetrievalAugmentationAdvisor.builder()
                // pre-retrieval
                .queryTransformers(TranslationQueryTransformer.builder()
                        .chatClientBuilder(chatClientBuilder.clone())
                        .targetLanguage("English")
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(3)
                        .similarityThreshold(0.5d)
                        .build())
                .documentPostProcessors(PIIMaskingDocumentPostProcessor.builder())
                .build();
    }

}
