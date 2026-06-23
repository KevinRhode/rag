package com.kevinrhode.rag.security;

public record AttackCase(
        String id,
        String owaspCategory,
        String vector,          // DIRECT or INDIRECT
        String payload,         // for DIRECT
        String poisonedContent, // for INDIRECT
        String benignQuery,     // for INDIRECT
        String canary) {}