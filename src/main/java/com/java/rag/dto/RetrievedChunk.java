package com.java.rag.dto;

import java.util.List;

/**
 * One retrieved chunk, exposed to the UI for transparency.
 *
 * @param source         originating document filename
 * @param similarity     cosine similarity in [0, 1] (null if the store didn't score it)
 * @param content        the chunk text
 * @param injectionFlags any injection phrases detected in this chunk
 */
public record RetrievedChunk(String source, Double similarity, String content, List<String> injectionFlags) {
}
