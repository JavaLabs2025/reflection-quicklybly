package org.example.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class Generator {

    // todo think about generic
    public Object generateValueOfType(
            Class<?> clazz
    ) throws InvocationTargetException, InstantiationException, IllegalAccessException, GenerationException {

        if (!clazz.isAnnotationPresent(Generatable.class)) {
            throw new GenerationException("Class is not annotated with @Generatable");
        }

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        int randomConstructorIndex = new Random().nextInt(constructors.length);
        Constructor<?> randomConstructor = constructors[randomConstructorIndex];
        return randomConstructor.newInstance(111);
    }
}
