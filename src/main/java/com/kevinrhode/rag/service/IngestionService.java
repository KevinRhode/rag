package com.kevinrhode.rag.service;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * Ingestion pipeline. Compared with a hand-rolled stack, Spring AI collapses three
 * steps into one: {@link VectorStore#add} runs each chunk through the configured
 * EmbeddingModel and writes the vector + content + metadata to pgvector. We never
 * call the embedding API or write SQL by hand.
 *
 * <p>Flow: read each markdown file -> split into chunks with {@link TokenTextSplitter}
 * -> {@code vectorStore.add(chunks)}.
 */
@Service
public class IngestionService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder().build();

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** Ingest every markdown file under {@code src/main/resources/docs/}. */
    public int ingestClasspathDocs() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:/docs/*.md");

        int total = 0;
        for (Resource resource : resources) {
            TextReader reader = new TextReader(resource);
            // Tag each chunk with its origin so retrieved results can cite a source.
            reader.getCustomMetadata().put("source", resource.getFilename());

            List<Document> documents = reader.get();
            List<Document> chunks = splitter.apply(documents);
            vectorStore.add(chunks);
            total += chunks.size();
        }
        return total;
    }
}
