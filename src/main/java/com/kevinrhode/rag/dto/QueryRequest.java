package com.kevinrhode.rag.dto;

/** Request body for POST /api/query. {@code topK} is optional (falls back to config). */
public record QueryRequest(String question, Integer topK) {
}
