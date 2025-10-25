package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;
import org.example.generator.type.TypeGeneratorsProvider;

public class Generator {

    private final Map<Class<?>, Supplier<?>> generators;

    private final int maxDepth;
    private final String packageToScan;

    private final Random random = new Random();

    public Generator(
            Collection<TypeGeneratorsProvider> providers,
            int maxDepth,
            Object packageMarker
    ) {
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

        this.packageToScan = packageMarker.getClass().getPackageName();
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

        // todo primitives cannot be null
        if (depth > maxDepth) {
            return null;
        }

        if (generators.containsKey(clazz)) {
            return generators.get(clazz).get();
        }

        if (clazz.isEnum()) {
            return generateEnum(clazz);
        }

        if (clazz.isArray()) {
            return generateArray(clazz, depth);
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            return generateCollectionFromClass(clazz);
        }

        if (Map.class.isAssignableFrom(clazz)) {
            return generateMapFromClass(clazz);
        }

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            try {
                return tryConstructor(constructor, depth);
            } catch (Exception e) {
                if (i == constructors.length - 1) {
                    throw e;
                }
            }
        }

        throw new GenerationException("No suitable constructor found for class: " + clazz.getName());
    }

    private boolean canBeGenerated(Class<?> clazz) {
        return generators.containsKey(clazz) ||
                clazz.isEnum() ||
                clazz.isArray() ||
                Collection.class.isAssignableFrom(clazz) ||
                Map.class.isAssignableFrom(clazz) ||
                clazz.isAnnotationPresent(Generatable.class);
    }

    // todo вынести длину в параметр
    private Object generateArray(
            Class<?> arrayClass,
            int depth
    ) throws GenerationException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> arrayElementClass = arrayClass.getComponentType();

        if (arrayElementClass == null) {
            throw new IllegalStateException("generateArray received not array as a parameter");
        }

        int length = random.nextInt(1, 10);
        Object result = Array.newInstance(arrayElementClass, length);

        for (int i = 0; i < length; ++i) {
            Object element = generateValueOfType(arrayElementClass, depth + 1);
            Array.set(result, i, element);
        }

        return result;
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

    private Collection<Object> generateCollectionFromClass(Class<?> collectionClass) {
        return switch (collectionClass) {
            case Class<?> c when Set.class.isAssignableFrom(c) -> new HashSet<>();
            case Class<?> c when Queue.class.isAssignableFrom(c) -> new LinkedList<>();
            default -> new ArrayList<>();
        };
    }

    private Map<Object, Object> generateMapFromClass(Class<?> mapClass) {
        if (SortedMap.class.isAssignableFrom(mapClass)) {
            return new TreeMap<>();
        }
        return new HashMap<>();
    }

    private Object tryConstructor(
            Constructor<?> constructor,
            int depth
    ) throws GenerationException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object[] paramValues = new Object[constructor.getParameterCount()];

        for (int i = 0; i < constructor.getParameterCount(); i++) {
            Class<?> paramType = constructor.getParameterTypes()[i];
            paramValues[i] = generateValueOfType(paramType, depth + 1);
        }

        var instance = constructor.newInstance(paramValues);

        var clazz = instance.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }

            field.setAccessible(true);
            Class<?> fieldClass = field.getType();

            Object fieldValue = switch (fieldClass) {
                case Class<?> c when Collection.class.isAssignableFrom(c) -> generateCollectionFromField(field, depth);
                case Class<?> c when Map.class.isAssignableFrom(c) -> generateMapFromField(field, depth);
                default -> generateValueOfType(fieldClass, depth + 1);
            };

            field.set(instance, fieldValue);
        }

        return instance;
    }

    private Collection<?> generateCollectionFromField(
            Field collectionField,
            int depth
    ) throws GenerationException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Type genericType = collectionField.getGenericType();
        Collection<Object> collection = generateCollectionFromClass(collectionField.getType());

        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();

            // if not generic, then fill
            if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?> elementType) {
                int length = random.nextInt(1, 10);

                for (int i = 0; i < length; ++i) {
                    Object element = generateValueOfType(elementType, depth + 1);
                    collection.add(element);
                }
            }
        }

        return collection;
    }

    private Map<?, ?> generateMapFromField(
            Field mapField,
            int depth
    ) throws GenerationException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Type genericType = mapField.getGenericType();
        Map<Object, Object> map = generateMapFromClass(mapField.getType());

        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();

            if (typeArgs.length == 2 &&
                    typeArgs[0] instanceof Class<?> keyType &&
                    typeArgs[1] instanceof Class<?> valueType) {

                int size = random.nextInt(1, 10);

                for (int i = 0; i < size; i++) {
                    Object key = generateValueOfType(keyType, depth + 1);
                    Object value = generateValueOfType(valueType, depth + 1);
                    map.put(key, value);
                }
            }
        }

        return map;
    }
}
