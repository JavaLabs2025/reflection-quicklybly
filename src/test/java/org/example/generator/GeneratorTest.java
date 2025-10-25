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
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorTest {

    private final Random random = new Random();

    private final Collection<TypeGeneratorsProvider> providers = List.of(
            new PrimitiveGeneratorsProvider(random),
            new StringGeneratorsProvider(random, 15)
    );

    private final Generator generator = new Generator(providers, Integer.MAX_VALUE);

    private enum TestEnum {
        ONE("one"),
        TWO("two"),
        ;

        final String name;

        TestEnum(String name) {
            this.name = name;
        }
    }

    private enum EmptyEnum {}

    @Test
    void shouldThrowOnDuplicateGenerator() {
        TypeGeneratorsProvider provider1 = () -> Map.of(String.class, () -> "test-string");
        TypeGeneratorsProvider provider2 = () -> Map.of(String.class, () -> "test-string-2");

        Collection<TypeGeneratorsProvider> duplicateProviders = List.of(provider1, provider2);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Generator(duplicateProviders, 1)
        );

        assertThat(exception.getMessage())
                .contains("Multiple providers supply generator for type: java.lang.String");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void shouldThrowOnNegativeOrZeroMaxDepth(int maxDepth) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Generator(List.of(), maxDepth)
        );

        assertThat(exception.getMessage())
                .isEqualTo("maxDepth expected to be more than 0, but got " + maxDepth);
    }

    @Test
    void shouldThrowExceptionOnNonGeneratableClass() {
        var ex = assertThrows(
                GenerationException.class,
                () -> generator.generateValueOfType(NonGeneratable.class)
        );
        assertThat(ex.getMessage()).isEqualTo(
                "Class is not annotated with @Generatable and not a simple type"
        );
    }

    @Test
    void shouldGenerateSimpleClassesFromGenerators() {
        TypeGeneratorsProvider provider = () -> Map.of(String.class, () -> "test-string");
        Generator generator = new Generator(List.of(provider), 1);

        assertThat(generate(generator, String.class)).isEqualTo("test-string");
    }

    @Test
    void shouldGenerateEnum() {
        Generator generator = new Generator(List.of(), 1);

        assertThat(generate(generator, TestEnum.class)).isInstanceOf(TestEnum.class);
    }

    @Test
    void shouldThrowOnEmptyEnum() {
        var ex = assertThrows(
                GenerationException.class,
                () -> generator.generateValueOfType(EmptyEnum.class)
        );
        assertThat(ex.getMessage()).isEqualTo("enum '" + this.getClass().getName() + "$" +
                EmptyEnum.class.getSimpleName() + "' cannot generated, because values is empty"
        );
    }

    @Test
    void shouldGenerateArray() {
        Class<?> intArrayClass = int[].class;
        assertThat(generate(intArrayClass)).isInstanceOf(intArrayClass);
    }

    @Test
    void shouldGenerate2DArray() {
        Class<?> intArrayClass = int[][].class;
        assertThat(generate(intArrayClass)).isInstanceOf(intArrayClass);
    }

    @Test
    void shouldThrowIfMaxDepthReached() {
        TypeGeneratorsProvider provider = () -> Map.of(String.class, () -> "test-string");
        var generator = new Generator(List.of(provider), 1);
        var ex = assertThrows(
                GenerationException.class,
                () -> generator.generateValueOfType(String[][].class)
        );
        assertThat(ex.getMessage()).isEqualTo("maxDepth exceeded");
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
        return generate(generator, clazz);
    }

    private Object generate(Generator generator, Class<?> clazz) {
        try {
            return generator.generateValueOfType(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
