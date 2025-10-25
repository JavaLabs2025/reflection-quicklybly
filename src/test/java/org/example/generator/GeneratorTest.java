package org.example.generator;

import java.util.*;
import org.example.classes.*;
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

    private final Generator generator = new Generator(providers, 10);

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
        assertThat(ex.getMessage()).isEqualTo("enum '" + EmptyEnum.class.getName() +
                "' cannot generated, because values is empty"
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
    void shouldPlaceNullIfMaxDepthReached() {
        TypeGeneratorsProvider provider = () -> Map.of(String.class, () -> "test-string");
        var generator = new Generator(List.of(provider), 1);
        var result = generate(generator, String[][].class);

        assertThat(result).isInstanceOf(String[][].class);

        var resultTyped = (String[][]) result;
        assertThat(resultTyped[0][0]).isNull();
    }

    @Test
    void shouldGenerateList() {
        var result = generate(List.class);
        assertThat(result).isInstanceOf(ArrayList.class);
        assertThat(((List<?>) result).size()).isEqualTo(0);
    }

    @Test
    void shouldGenerateSet() {
        var result = generate(Set.class);
        assertThat(result).isInstanceOf(HashSet.class);
        assertThat(((Set<?>) result).size()).isEqualTo(0);
    }

    @Test
    void shouldGenerateQueue() {
        var result = generate(Queue.class);
        assertThat(result).isInstanceOf(LinkedList.class);
        assertThat(((Queue<?>) result).size()).isEqualTo(0);
    }

    @Test
    void shouldGenerateCollection() {
        var result = generate(Collection.class);
        assertThat(result).isInstanceOf(ArrayList.class);
        assertThat(((Collection<?>) result).size()).isEqualTo(0);
    }

    @Test
    void shouldGenerateMap() {
        var result = generate(Map.class);
        assertThat(result).isInstanceOf(HashMap.class);
        assertThat(((Map<?, ?>) result).size()).isEqualTo(0);
    }

    @Test
    void shouldGenerateSortedMap() {
        var result = generate(SortedMap.class);
        assertThat(result).isInstanceOf(TreeMap.class);
        assertThat(((Map<?, ?>) result).size()).isEqualTo(0);
    }

    @Test
    void shouldFillInternalCollection() {
        var instance = generate(Cart.class);
        assertThat(instance).isInstanceOf(Cart.class);

        var cart = (Cart) instance;
        var items = cart.getItems();
        assertThat(items.size()).isNotEqualTo(0);
        assertThat(items.getFirst()).isInstanceOf(Product.class);
    }

    @Test
    void shouldFillInternalMap() {
        var instance = generate(InternalMapTest.class);
        assertThat(instance).isInstanceOf(InternalMapTest.class);

        var cart = (InternalMapTest) instance;
        var items = cart.getItems();
        assertThat(items.size()).isNotEqualTo(0);

        Map.Entry<?, ?> item = items.entrySet().iterator().next();
        assertThat(item.getKey()).isInstanceOf(Product.class);
        assertThat(item.getValue()).isInstanceOf(String.class);
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
                Cart.class,
                BinaryTreeNode.class,
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
