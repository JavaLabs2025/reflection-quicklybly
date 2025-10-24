package org.example.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import org.example.generator.type.TypeGeneratorsProvider;

public class Generator {

    private final Map<Class<?>, Supplier<?>> generators;

    private final int maxDepth;

    private final Random random = new Random();

    public Generator(Collection<TypeGeneratorsProvider> providers, int maxDepth) {
        Map<Class<?>, Supplier<?>> result = new HashMap<>();

        for (TypeGeneratorsProvider provider : providers) {
            Map<Class<?>, Supplier<?>> generatorsFromProvider = provider.getGenerators();

            for (Map.Entry<Class<?>, Supplier<?>> entry : generatorsFromProvider.entrySet()) {
                Class<?> type = entry.getKey();
                Supplier<?> supplier = entry.getValue();

                if (result.containsKey(type)) {
                    throw new IllegalArgumentException(
                            "Multiple providers supply generator for type: " + type.getName()
                    );
                }

                result.put(type, supplier);
            }
        }

        this.generators = Map.copyOf(result);

        if (maxDepth <= 0) {
            throw new IllegalArgumentException("maxDepth expected to be more than 0, but got " + maxDepth);
        }
        this.maxDepth = maxDepth;
    }

    public Object generateValueOfType(
            Class<?> clazz
    ) throws InvocationTargetException, InstantiationException, IllegalAccessException, GenerationException {
        return generateValueOfType(clazz, 0);
    }

    private Object generateValueOfType(
            Class<?> clazz,
            int depth
    ) throws InvocationTargetException, InstantiationException, IllegalAccessException, GenerationException {

        if (!canBeGenerated(clazz)) {
            throw new GenerationException(
                    "Class is not annotated with @" + Generatable.class.getSimpleName() + " and not a simple type"
            );
        }

        // todo add tests
        if (depth > maxDepth) {
            throw new GenerationException("maxDepth exceeded");
        }

        if (generators.containsKey(clazz)) {
            return generators.get(clazz).get();
        }

        if (clazz.isEnum()) {
            return generateEnum(clazz);
        }

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            try {
                return tryConstructor(constructor);
            } catch (Exception e) {
                if (i == constructors.length - 1) {
                    throw e;
                }
            }
        }

        throw new GenerationException("No suitable constructor found for class: " + clazz.getName());
    }

    private boolean canBeGenerated(Class<?> clazz) {
        return generators.containsKey(clazz) || clazz.isEnum() ||
                clazz.isAnnotationPresent(Generatable.class);
    }

    private Object generateEnum(Class<?> enumClass) throws GenerationException {
        Object[] values = enumClass.getEnumConstants();

        if (values == null) {
            // should not happen
            throw new IllegalStateException("generateEnum received not enum as a parameter");
        }

        if (0 == values.length) {
            throw new GenerationException("enum '" + enumClass.getName() +
                    "' cannot generated, because values is empty"
            );
        }

        return values[random.nextInt(values.length)];
    }

    private Object tryConstructor(
            Constructor<?> constructor
    ) throws GenerationException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object[] paramValues = new Object[constructor.getParameterCount()];

        for (int i = 0; i < constructor.getParameterCount(); i++) {
            Class<?> paramType = constructor.getParameterTypes()[i];
            Supplier<?> supplier = generators.get(paramType);

            if (supplier == null) {
                throw new GenerationException("Unknown type: " + paramType.getName());
            }

            paramValues[i] = supplier.get();
        }

        return constructor.newInstance(paramValues);
    }
}
