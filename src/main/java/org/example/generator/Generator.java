package org.example.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.example.generator.type.TypeGeneratorsProvider;

public class Generator {

    private final Map<Class<?>, Supplier<?>> generators;

    public Generator(Collection<TypeGeneratorsProvider> providers) {
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
    }

    public Object generateValueOfType(
            Class<?> clazz
    ) throws InvocationTargetException, InstantiationException, IllegalAccessException, GenerationException {

        if (!clazz.isAnnotationPresent(Generatable.class)) {
            throw new GenerationException("Class is not annotated with @Generatable");
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
