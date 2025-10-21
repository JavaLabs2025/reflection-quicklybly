package org.example.generator.type.impl;

import java.util.Random;
import org.example.generator.type.TypeGeneratorsProvider;
import org.example.generator.type.TypeGeneratorsProviderTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class StringGeneratorsProviderTest extends TypeGeneratorsProviderTest {

    private final Random random = new Random();
    private final TypeGeneratorsProvider provider = new StringGeneratorsProvider(random, 15);

    @Override
    protected TypeGeneratorsProvider getProvider() {
        return provider;
    }

    @Test
    void shouldGenerateStrings() {
        var generators = provider.getGenerators();

        assertThat(generators.size()).isEqualTo(1);
        assertThat(generators.containsKey(String.class)).isTrue();
    }
}
