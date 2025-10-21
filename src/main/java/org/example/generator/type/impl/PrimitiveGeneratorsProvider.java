package org.example.generator.type.impl;

import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import org.example.generator.type.TypeGeneratorsProvider;

public class PrimitiveGeneratorsProvider implements TypeGeneratorsProvider {

    private final Random random;

    public PrimitiveGeneratorsProvider(Random random) {
        this.random = random;
    }

    @Override
    public Map<Class<?>, Supplier<?>> getGenerators() {

        return Map.ofEntries(
                Map.entry(boolean.class, random::nextBoolean),
                Map.entry(Boolean.class, random::nextBoolean),

                Map.entry(byte.class, () -> (byte) random.nextInt(Byte.MAX_VALUE)),
                Map.entry(Byte.class, () -> (byte) random.nextInt(Byte.MAX_VALUE)),

                Map.entry(short.class, () -> (short) random.nextInt(Short.MAX_VALUE)),
                Map.entry(Short.class, () -> (short) random.nextInt(Short.MAX_VALUE)),

                Map.entry(int.class, random::nextInt),
                Map.entry(Integer.class, random::nextInt),

                Map.entry(long.class, random::nextLong),
                Map.entry(Long.class, random::nextLong),

                Map.entry(float.class, random::nextFloat),
                Map.entry(Float.class, random::nextFloat),

                Map.entry(double.class, random::nextDouble),
                Map.entry(Double.class, random::nextDouble),

                Map.entry(char.class, () -> (char) random.nextInt(Character.MAX_VALUE)),
                Map.entry(Character.class, () -> (char) random.nextInt(Character.MAX_VALUE))
        );
    }
}
