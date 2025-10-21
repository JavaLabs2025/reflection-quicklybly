package org.example.generator.type;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public abstract class TypeGeneratorsProviderTest {

    protected abstract TypeGeneratorsProvider getProvider();

    @Test
    void suppliersShouldMatchClasses() {
        var provider = getProvider();

        var generators = provider.getGenerators();

        for (var classToSupplier : generators.entrySet()) {
            var clazz = classToSupplier.getKey();
            var supplier = classToSupplier.getValue();

            var instance = supplier.get();

            assertThat(instance).isInstanceOfAny(clazz, getPrimitiveType(clazz), getWrapperType(clazz));
        }
    }

    private Class<?> getPrimitiveType(Class<?> wrapper) {
        return switch (wrapper.getSimpleName()) {
            case "java.lang.Integer" -> int.class;
            case "java.lang.Short" -> short.class;
            case "java.lang.Byte" -> byte.class;
            case "java.lang.Long" -> long.class;
            case "java.lang.Float" -> float.class;
            case "java.lang.Double" -> double.class;
            case "java.lang.Character" -> char.class;
            case "java.lang.Boolean" -> boolean.class;
            case "java.lang.Void" -> void.class;
            default -> wrapper;
        };
    }

    private Class<?> getWrapperType(Class<?> primitive) {
        return switch (primitive.getSimpleName()) {
            case "int" -> Integer.class;
            case "short" -> Short.class;
            case "byte" -> Byte.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "char" -> Character.class;
            case "boolean" -> Boolean.class;
            case "void" -> Void.class;
            default -> primitive;
        };
    }
}
