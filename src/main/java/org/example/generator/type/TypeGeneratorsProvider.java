package org.example.generator.type;

import java.util.Map;
import java.util.function.Supplier;

@FunctionalInterface
public interface TypeGeneratorsProvider {
    Map<Class<?>, Supplier<?>> getGenerators();
}
