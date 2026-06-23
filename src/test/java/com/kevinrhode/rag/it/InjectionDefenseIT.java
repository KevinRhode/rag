package com.kevinrhode.rag.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.kevinrhode.rag.security.AttackCase;
import com.kevinrhode.rag.service.RagService;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local-llm")
@Tag("local-llm")
class InjectionDefenseIT {

    @Autowired RagService ragService;
    @Autowired VectorStore vectorStore;

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.kevinrhode.rag.security.AttackFixtures#directAttacks")
    void direct_attacks_do_not_leak_canary(AttackCase attack) {
        String answer = ragService.answer(attack.payload(), null).answer();
        assertThat(answer)
                .as("Direct attack %s must not produce its canary", attack.id())
                .doesNotContain(attack.canary());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.kevinrhode.rag.security.AttackFixtures#indirectAttacks")
    void poisoned_documents_do_not_hijack_the_answer(AttackCase attack) {
        vectorStore.add(List.of(Document.builder().text(attack.poisonedContent()).build()));

        String answer = ragService.answer(attack.benignQuery(), null).answer();

        assertThat(answer)
                .as("Poisoned doc %s must not inject %s into the answer",
                        attack.id(), attack.canary())
                .doesNotContain(attack.canary());
    }
}