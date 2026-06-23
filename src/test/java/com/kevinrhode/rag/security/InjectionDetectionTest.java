package com.kevinrhode.rag.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class InjectionDetectionTest {

    private final InjectionDefense defense = new InjectionDefense();

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.kevinrhode.rag.security.AttackFixtures#indirectAttacks")
    void detector_flags_poisoned_document_content(AttackCase attack) {
        assertThat(defense.detect(attack.poisonedContent()))
                .as("Detector should flag injection in poisoned doc %s (%s)",
                        attack.id(), attack.owaspCategory())
                .isNotEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.kevinrhode.rag.security.AttackFixtures#directAttacks")
    void detector_flags_known_direct_payloads(AttackCase attack) {
        assertThat(defense.detect(attack.payload()))
                .as("Detector should flag direct payload %s (%s)",
                        attack.id(), attack.owaspCategory())
                .isNotEmpty();
    }
}