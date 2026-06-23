package com.kevinrhode.rag.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Second-stage reranker. Vector similarity is a coarse first pass; this re-scores the
 * candidate set with full query/passage attention via the chat model, then keeps the
 * top-k. Uses ChatModel directly (terse prompt, temperature 0, no RAG system prompt) and
 * fails open to the original vector order so reranking can never break retrieval.
 */
@Component
public class RerankingRetriever {

    private final ChatClient rerankClient;

    public RerankingRetriever(ChatModel chatModel) {
        this.rerankClient = ChatClient.builder(chatModel).build();
    }

    public List<Document> rerank(String query, List<Document> candidates, int k) {
        if (candidates.size() <= k) {
            return candidates;
        }

        String numbered = IntStream.range(0, candidates.size())
                .mapToObj(i -> i + ": " + cleanText(candidates.get(i)))
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                You are a search re-ranker. Rank the passages by how well they answer the query.
                Return ONLY a comma-separated list of passage numbers, most relevant first.
                Do not explain. Example: 3,0,7,1

                Query: %s

                Passages:
                %s
                """.formatted(query, numbered);

        String ranked;
        try {
            ranked = rerankClient.prompt()
                    .user(prompt)
                    .options(ChatOptions.builder().temperature(0.0))
                    .call()
                    .content();
        } catch (Exception e) {
            return new ArrayList<>(candidates.subList(0, k));
        }

        // qwen3 is a reasoning model and may emit <think> blocks — strip before parsing.
        ranked = ranked.replaceAll("(?s)<think>.*?</think>", "").trim();

        List<Document> ordered = new ArrayList<>();
        for (String token : ranked.split(",")) {
            String t = token.trim();
            if (t.matches("\\d+")) {
                int idx = Integer.parseInt(t);
                if (idx >= 0 && idx < candidates.size() && !ordered.contains(candidates.get(idx))) {
                    ordered.add(candidates.get(idx));
                }
            }
            if (ordered.size() == k) break;
        }
        // Top up from vector order if the model under-returned or returned garbage.
        for (Document d : candidates) {
            if (ordered.size() == k) break;
            if (!ordered.contains(d)) ordered.add(d);
        }
        return ordered;
    }

    static String cleanText(Document doc) {
        Object clean = doc.getMetadata().get("clean_text");
        return clean != null ? clean.toString() : doc.getText();
    }
}