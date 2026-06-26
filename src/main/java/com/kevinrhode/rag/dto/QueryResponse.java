package com.kevinrhode.rag.dto;

import java.util.List;

/** Response for POST /api/query: the grounded answer plus the evidence behind it. */
public record QueryResponse(String answer, List<RetrievedChunk> retrieved, List<RetrievedChunk> quarantined, boolean injectionDefense) {
}
