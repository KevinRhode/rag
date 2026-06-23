package com.kevinrhode.rag.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.ClassPathResource;

public final class AttackFixtures {

    private AttackFixtures() {}

    private static List<AttackCase> loadAll() {
        var mapper = new ObjectMapper();
        try (var in = new ClassPathResource("attacks/injection-cases.json").getInputStream()) {
            return mapper.readValue(in, new TypeReference<List<AttackCase>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load attacks/injection-cases.json", e);
        }
    }

    static Stream<AttackCase> directAttacks() {
        return loadAll().stream().filter(a -> "DIRECT".equals(a.vector()));
    }

    static Stream<AttackCase> indirectAttacks() {
        return loadAll().stream().filter(a -> "INDIRECT".equals(a.vector()));
    }
}