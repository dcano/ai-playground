package io.twba.aiplayground.rag;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HRPolicyLoader {

    private final VectorStore vectorStore;

    @Value("classpath:Eazybytes_HR_Policies.pdf")
    Resource policyFile;

    @Autowired
    public HRPolicyLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadPDF() {
        TextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(100).withMaxNumChunks(400).build();
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(policyFile);
        List<Document> documents = tikaDocumentReader.get().stream()
                .map(d -> new Document(d.getFormattedContent(),
                        Map.of("category", "document")))
                .toList();
        vectorStore.add(textSplitter.split(documents));
    }

    @PreDestroy
    public void destroy() {
        vectorStore.delete("category == 'document'");
    }
}
