package com.kevinrhode.rag.service;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Ingest every markdown file under {@code src/main/resources/docs/} and remote repository.
     */
    public int ingestDocs() throws IOException {
        int total = 0;

        // Fetch local resources
        total += ingestLocalDocs();

        // Fetch remote resources
        total += ingestRemoteDocs();

        return total;
    }

    private int ingestLocalDocs() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:/docs/*.md");

        int localTotal = 0;
        for (Resource resource : resources) {
            TextReader reader = new TextReader(resource);
            reader.getCustomMetadata().put("source", resource.getFilename());

            List<Document> documents = reader.get();
            List<Document> chunks = splitter.apply(documents);

            List<Document> prefixed = new ArrayList<>(chunks.size());
            for (Document chunk : chunks) {
                String clean = chunk.getText();
                Map<String, Object> meta = new HashMap<>(chunk.getMetadata());
                meta.put("clean_text", clean);
                prefixed.add(Document.builder()
                        .text("search_document: " + clean)
                        .metadata(meta)
                        .build());
            }

            vectorStore.add(prefixed);
            localTotal += prefixed.size();
        }
        return localTotal;
    }

    private int ingestRemoteDocs() throws IOException {
        List<String> remoteDocUrls = search_repositories(); // Assuming this function returns a list of URLs

        int remoteTotal = 0;
        for (String url : remoteDocUrls) {
            URL remoteUrl = new URL(url);
            String content = fetchContentFromUrl(remoteUrl);

            Document document = Document.builder()
                    .text(content)
                    .metadata(Map.of("source", "remote"))
                    .build();

            List<Document> chunks = splitter.apply(List.of(document));

            List<Document> prefixed = new ArrayList<>(chunks.size());
            for (Document chunk : chunks) {
                String clean = chunk.getText();
                Map<String, Object> meta = new HashMap<>(chunk.getMetadata());
                meta.put("clean_text", clean);
                prefixed.add(Document.builder()
                        .text("search_document: " + clean)
                        .metadata(meta)
                        .build());
            }

            vectorStore.add(prefixed);
            remoteTotal += prefixed.size();
        }
        return remoteTotal;
    }


    private String fetchContentFromUrl(URL url) throws IOException {
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    // Placeholder for search_repositories function
    private List<String> search_repositories() {
        // Implement the logic to search repositories and return a list of URLs
        return List.of(); // Replace with actual implementation
    }

    /** Ingest every markdown file under {@code src/main/resources/docs/}. */
    public int ingestClasspathDocs() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:/docs/*.md");

        int total = 0;
        for (Resource resource : resources) {
            TextReader reader = new TextReader(resource);
            reader.getCustomMetadata().put("source", resource.getFilename());

            List<Document> documents = reader.get();
            List<Document> chunks = splitter.apply(documents);

            List<Document> prefixed = new ArrayList<>(chunks.size());
            for (Document chunk : chunks) {
                String clean = chunk.getText();
                Map<String, Object> meta = new HashMap<>(chunk.getMetadata());
                meta.put("clean_text", clean);
                prefixed.add(Document.builder()
                        .text("search_document: " + clean)
                        .metadata(meta)
                        .build());
            }

            vectorStore.add(prefixed);
            total += prefixed.size();
        }
        return total;
    }
}
