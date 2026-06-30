package com.kevinrhode.rag.controller;

import java.io.IOException;
import java.util.Map;

import com.kevinrhode.rag.dto.QueryRequest;
import com.kevinrhode.rag.dto.QueryResponse;
import com.kevinrhode.rag.service.IngestionService;
import com.kevinrhode.rag.service.RagService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RagController {

    private final RagService ragService;
    private final IngestionService ingestionService;

    public RagController(RagService ragService, IngestionService ingestionService) {
        this.ragService = ragService;
        this.ingestionService = ingestionService;
    }

    @GetMapping("/api/healthz")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /** Build the index from the bundled corpus. Idempotency is a roadmap item (see README). */
    @PostMapping("/api/ingest")
    public Map<String, Object> ingest() throws IOException {
        int chunks = ingestionService.ingestDocs();
        return Map.of("ingestedChunks", chunks);
    }

    @PostMapping("/api/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }
        return ragService.answer(request.question(), request.topK());
    }
}
