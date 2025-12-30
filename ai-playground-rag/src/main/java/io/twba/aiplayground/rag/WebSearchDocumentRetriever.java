package io.twba.aiplayground.rag;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

import java.util.List;

public class WebSearchDocumentRetriever implements DocumentRetriever {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchDocumentRetriever.class);

    private static final String TAVILY_BASE_URL = "https://api.tavily.com/search";
    private final int resultLimit;
    private final RestClient restClient;
    ;

    private WebSearchDocumentRetriever(String apiKey, int resultLimit, RestClient.Builder restClientBuilder) {
        Assert.hasText(apiKey, "tavily api key must not be empty");
        Assert.notNull(restClientBuilder, "restClientBuilder must not be null");
        this.resultLimit = resultLimit;
        this.restClient = restClientBuilder
                .baseUrl(TAVILY_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public List<Document> retrieve(Query query) {
        logger.info("Processing query: {}", query.text());
        Assert.notNull(query, "query cannot be null");
        String q = query.text();
        Assert.hasText(q, "query.text() cannot be empty");

        TavilyResponsePayload  response = restClient.post()
                .body(new TavilyRequestPayload(q, "advanced", resultLimit))
                .retrieve()
                .body(TavilyResponsePayload.class);

        if(response == null || response.results().isEmpty()) {
            return List.of();
        }

        return response.results().stream().map(hit -> Document.builder()
                .text(hit.content())
                .metadata("title", hit.title())
                .metadata("url", hit.url())
                .score(hit.score())
                .build()).toList();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record TavilyRequestPayload(String query, String searchDepth, int maxResults) {}

    record TavilyResponsePayload(List<Hit> results) {
        record Hit(String title, String url, String content, Double score) {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RestClient.Builder restClientBuilder;
        private String tavilyApiKey;
        private int resultLimit;

        private Builder() {}

        public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            this.restClientBuilder = restClientBuilder;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.tavilyApiKey = apiKey;
            return this;
        }

        public Builder maxResults(int maxResults) {
            if(maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be greater than 0");
            }
            this.resultLimit = maxResults;
            return this;
        }

        public WebSearchDocumentRetriever build() {
            return new WebSearchDocumentRetriever(tavilyApiKey, resultLimit, restClientBuilder);
        }
    }
}
