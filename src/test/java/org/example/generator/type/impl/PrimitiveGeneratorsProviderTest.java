package org.example.generator.type.impl;

import java.util.Random;
import org.example.generator.type.TypeGeneratorsProvider;
import org.example.generator.type.TypeGeneratorsProviderTest;

class PrimitiveGeneratorsProviderTest extends TypeGeneratorsProviderTest {

    private final Random random = new Random();
    private final TypeGeneratorsProvider provider = new PrimitiveGeneratorsProvider(random);

    @Override
    protected TypeGeneratorsProvider getProvider() {
        return provider;
    }
}
