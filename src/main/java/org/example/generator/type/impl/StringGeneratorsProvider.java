package org.example.generator.type.impl;

import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import org.example.generator.type.TypeGeneratorsProvider;

public class StringGeneratorsProvider implements TypeGeneratorsProvider {

    private final Supplier<String> stringSupplier;

    public StringGeneratorsProvider(Random random, int maxLength) {

        this.stringSupplier = () -> {
            int length = random.nextInt(maxLength);
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append((char) (random.nextInt(26) + 'a'));
            }

            return sb.toString();
        };
    }

    @Override
    public Map<Class<?>, Supplier<?>> getGenerators() {

        return Map.of(String.class, stringSupplier);
    }
}
