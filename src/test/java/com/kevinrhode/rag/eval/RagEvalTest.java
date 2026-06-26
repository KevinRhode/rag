package com.kevinrhode.rag.eval;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinrhode.rag.dto.QueryResponse;
import com.kevinrhode.rag.dto.RetrievedChunk;
import com.kevinrhode.rag.service.RagService;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Content;          // if this import is red, let the IDE find Content
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local-llm")
@Tag("eval")                       // so CI can exclude it (needs the local Ollama box)
class RagEvalTest {

    @Autowired RagService ragService;
    @Autowired ChatModel chatModel;

    record EvalCase(String question, List<String> expectedSources) {}

    @Test
    void evaluate_rag_quality() throws IOException {
        List<EvalCase> cases = loadCases();

        var relevancy = new RelevancyEvaluator(ChatClient.builder(chatModel));
//        var grounding = FactCheckingEvaluator.forBespokeMinicheck(ChatClient.builder(chatModel));

        int hits = 0, relevant = 0, grounded = 0;
        double reciprocalRankSum = 0;

        for (EvalCase c : cases) {
            QueryResponse resp = ragService.answer(c.question(), null);
            List<RetrievedChunk> retrieved = resp.retrieved();

            // --- retrieval metric: where did the expected source land? ---
            int rank = -1;
            for (int i = 0; i < retrieved.size(); i++) {
                if (c.expectedSources().contains(retrieved.get(i).source())) {
                    rank = i + 1;
                    break;
                }
            }
            if (rank > 0) { hits++; reciprocalRankSum += 1.0 / rank; }

            // --- answer metrics: relevancy + groundedness (LLM judged) ---
            List<Document> context = retrieved.stream()
                    .map(rc -> Document.builder().text(rc.content()).build())
                    .toList();
            var request = new EvaluationRequest(c.question(), context, resp.answer());
            if (relevancy.evaluate(request).isPass()) relevant++;
            if (isGrounded(resp.answer(), context)) grounded++;
//            if (grounding.evaluate(request).isPass()) grounded++;

            System.out.printf("rank=%-5s | %s%n", rank > 0 ? rank : "MISS", c.question());
        }

        int n = cases.size();
        System.out.println("\n==================== RAG EVAL ====================");
        System.out.printf("hit@k:     %.2f   (right doc retrieved at all)%n", (double) hits / n);
        System.out.printf("MRR:       %.2f   (right doc near the top)%n", reciprocalRankSum / n);
        System.out.printf("relevant:  %.0f%%    (answer addressed the question)%n", 100.0 * relevant / n);
        System.out.printf("grounded:  %.0f%%    (answer supported by context)%n", 100.0 * grounded / n);
        System.out.println("==================================================");
    }

    private List<EvalCase> loadCases() throws IOException {
        var mapper = new ObjectMapper();
        try (var in = new ClassPathResource("eval/rag-eval-set.json").getInputStream()) {
            return mapper.readValue(in, new TypeReference<List<EvalCase>>() {});
        }
    }

    private boolean isGrounded(String answer, List<Document> context) {
        String ctx = context.stream()
                .map(Document::getText)
                .collect(java.util.stream.Collectors.joining("\n---\n"));

        String prompt = """
            You are a strict fact-checker. Given the CONTEXT and an ANSWER, decide whether
            the answer is supported by the context. Reply with exactly one word: YES or NO.

            CONTEXT:
            %s

            ANSWER:
            %s
            """.formatted(ctx, answer);

        String verdict = ChatClient.builder(chatModel).build()
                .prompt().user(prompt).call().content();

        verdict = verdict.replaceAll("(?s)<think>.*?</think>", "").trim().toUpperCase();
        return verdict.startsWith("YES");
    }
}