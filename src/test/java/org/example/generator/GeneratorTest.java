package org.example.generator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.example.classes.Example;
import org.example.classes.NonGeneratable;
import org.example.classes.Product;
import org.example.classes.Rectangle;
import org.example.classes.Triangle;
import org.example.generator.type.TypeGeneratorsProvider;
import org.example.generator.type.impl.PrimitiveGeneratorsProvider;
import org.example.generator.type.impl.StringGeneratorsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorTest {

    private final Random random = new Random();

    private final Collection<TypeGeneratorsProvider> providers = List.of(
            new PrimitiveGeneratorsProvider(random),
            new StringGeneratorsProvider(random, 15)
    );

    private final Generator generator = new Generator(providers);

    @Test
    void shouldThrowOnDuplicateGenerator() {
        TypeGeneratorsProvider provider1 = () -> Map.of(String.class, () -> "test-string");
        TypeGeneratorsProvider provider2 = () -> Map.of(String.class, () -> "test-string-2");

        Collection<TypeGeneratorsProvider> duplicateProviders = List.of(provider1, provider2);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Generator(duplicateProviders)
        );

        assertThat(exception.getMessage())
                .contains("Multiple providers supply generator for type: java.lang.String");
    }

    @Test
    void shouldThrowExceptionOnNonGeneratableClass() {
        var ex = assertThrows(
                GenerationException.class,
                () -> generator.generateValueOfType(NonGeneratable.class)
        );
        assertThat(ex.getMessage()).isEqualTo("Class is not annotated with @Generatable");
    }

    @ParameterizedTest
    @MethodSource("source")
    void shouldGenerateSupportedClasses(Class<?> clazz) {
        var instance = generate(clazz);
        assertThat(instance).isInstanceOf(clazz);
    }

    static List<Class<?>> source() {
        return List.of(
                Example.class,
                Product.class,
                Rectangle.class,
                Triangle.class
        );
    }

    private Object generate(Class<?> clazz) {
        try {
            return generator.generateValueOfType(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
