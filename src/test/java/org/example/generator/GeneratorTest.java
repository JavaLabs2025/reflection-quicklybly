package org.example.generator;

import org.example.classes.Example;
import org.example.classes.NonGeneratable;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorTest {

    private final Generator generator = new Generator();

    @Test
    void shouldThrowExceptionOnNonGeneratableClass() {
        var ex = assertThrows(
                GenerationException.class,
                () -> generator.generateValueOfType(NonGeneratable.class)
        );
        assertThat(ex.getMessage()).isEqualTo("Class is not annotated with @Generatable");
    }

    @Test
    void shouldGenerateExampleClass() {
        var example = generate(Example.class);
        assertThat(example).isInstanceOf(Example.class);
    }

    private Object generate(Class<?> clazz) {
        try {
            return generator.generateValueOfType(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
