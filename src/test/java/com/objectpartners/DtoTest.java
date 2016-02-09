package com.objectpartners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * A utility class which allows for testing entity and transfer object classes. This is mainly for code coverage since
 * these types of objects are normally nothing more than setters and settings. If any logic exists in the method, then
 * the get method should be sent in to be ignored and a custom test function should be written.
 *
 * @param <T> The object type to test.
 */
public abstract class DtoTest<T> {

    private static final ImmutableMap<Class<?>, Supplier<?>> DEFAULT_MAPPERS;

    static {
        final Builder<Class<?>, Supplier<?>> mapperBuilder = ImmutableMap.builder();

        /* Primitives */
        mapperBuilder.put(int.class, () -> 0);
        mapperBuilder.put(double.class, () -> 0.0d);
        mapperBuilder.put(float.class, () -> 0.0f);
        mapperBuilder.put(long.class, () -> 0l);
        mapperBuilder.put(boolean.class, () -> true);
        mapperBuilder.put(short.class, () -> (short) 0);
        mapperBuilder.put(byte.class, () -> (byte) 0);
        mapperBuilder.put(char.class, () -> (char) 0);

        mapperBuilder.put(Integer.class, () -> Integer.valueOf(0));
        mapperBuilder.put(Double.class, () -> Double.valueOf(0.0));
        mapperBuilder.put(Float.class, () -> Float.valueOf(0.0f));
        mapperBuilder.put(Long.class, () -> Long.valueOf(0));
        mapperBuilder.put(Boolean.class, () -> Boolean.TRUE);
        mapperBuilder.put(Short.class, () -> Short.valueOf((short) 0));
        mapperBuilder.put(Byte.class, () -> Byte.valueOf((byte) 0));
        mapperBuilder.put(Character.class, () -> Character.valueOf((char) 0));

        mapperBuilder.put(BigDecimal.class, () -> BigDecimal.ONE);

        /* Collection Types. */
        mapperBuilder.put(Set.class, () -> Collections.emptySet());
        mapperBuilder.put(SortedSet.class, () -> Collections.emptySortedSet());
        mapperBuilder.put(List.class, () -> Collections.emptyList());
        mapperBuilder.put(Map.class, () -> Collections.emptyMap());
        mapperBuilder.put(SortedMap.class, () -> Collections.emptySortedMap());

        DEFAULT_MAPPERS = mapperBuilder.build();
    }

    /** The get fields to ignore and not try to test. */
    private final Set<String> ignoredGetFields;

    /**
     * A custom mapper. Normally used when the test class has abstract objects.
     */
    private final ImmutableMap<Class<?>, Supplier<?>> mappers;

    /**
     * Creates an instance of {@link TransferObjectTest} with the default ignore fields.
     */
    protected DtoTest() {
        this(null, null);
    }

    /**
     * Creates an instance of {@link TransferObjectTest} with the default ignore fields.
     *
     * @param capitalizedVariables If the variables in the test class start with a capital letter.
     * @param ignoreFields The getters which should be ignored (e.g., "getId" or "isActive").
     */
    protected DtoTest(Map<Class<?>, Supplier<?>> customMappers, Set<String> ignoreFields) {
        this.ignoredGetFields = new HashSet<>();
        if (ignoreFields != null) {
            this.ignoredGetFields.addAll(ignoreFields);
        }
        this.ignoredGetFields.add("getClass");

        if (customMappers == null) {
            this.mappers = DEFAULT_MAPPERS;
        } else {
            final Builder<Class<?>, Supplier<?>> builder = ImmutableMap.builder();
            builder.putAll(customMappers);
            builder.putAll(DEFAULT_MAPPERS);
            this.mappers = builder.build();
        }
    }

    /**
     * Calls a getter and verifies the result is what is expected.
     *
     * @param fieldName The field name (used for error messages).
     * @param getter The get {@link Method}.
     * @param instance The test instance.
     * @param fieldType The type of the return type.
     * @param expected The expected result.
     *
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private void callGetter(String fieldName, Method getter, T instance, Class<?> fieldType, Object expected)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final Object getResult = getter.invoke(instance);

        if (fieldType.isPrimitive()) {
            /*
             * Have to handle primitives explicitly. This is because this is a method and Java will perform autoboxing
             * and convert our primitives into objects.
             */
            if (Integer.class.equals(expected.getClass())) {
                assertEquals(fieldName + " is different", ((Integer) expected).intValue(), getResult);
            } else if (Double.class.equals(expected.getClass())) {
                assertEquals(fieldName + " is different", ((Double) expected).doubleValue(), getResult);
            } else if (Float.class.equals(expected.getClass())) {
                assertEquals(fieldName + " is different", ((Float) expected).floatValue(), getResult);
            } else if (Long.class.equals(expected.getClass())) {
                assertEquals(fieldName + " is different", ((Long) expected).longValue(), getResult);
            } else if (Boolean.class.equals(expected.getClass())) {
                assertEquals(fieldName + " is different", ((Boolean) expected).booleanValue(), getResult);
            } else if (Short.class.equals(expected.getClass())) {
                assertEquals(fieldName + " is different", ((Short) expected).shortValue(), getResult);
            } else if (Byte.class.equals(expected.getClass())) {
                assertEquals(fieldName + " is different", ((Byte) expected).byteValue(), getResult);
            } else if (Character.class.equals(expected.getClass())) {
                assertEquals(fieldName + " is different", ((Character) expected).charValue(), getResult);
            } else {
                fail("Unknown primitive type: " + fieldType);
            }

        } else {
            assertSame(fieldName + " is different", expected, getResult);
        }
    }

    /**
     * Creates an object for the given {@link Class}.
     *
     * @param clazz The {@link Class} type to create.
     *
     * @return A new instance for the given {@link Class}.
     *
     * @throws InstantiationException If this Class represents an abstract class, an interface, an array class, a
     *             primitive type, or void; or if the class has no nullary constructor; or if the instantiation fails
     *             for some other reason.
     * @throws IllegalAccessException If the class or its nullary constructor is not accessible.
     *
     */
    private Object createObject(String fieldName, Class<?> clazz)
            throws InstantiationException, IllegalAccessException {

        final Supplier<?> supplier = this.mappers.get(clazz);
        if (supplier != null) {
            return supplier.get();
        }

        if (clazz.isEnum()) {
            return clazz.getEnumConstants()[0];
        }

        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Unable to create objects for field '" + fieldName + "'.", e);
        }
    }

    /**
     * Returns an instance to use to test the get and set methods.
     *
     * @return An instance to use to test the get and set methods.
     */
    protected abstract T getInstance();

    /**
     * Tests all the getters and setters. Verifies that when a set method is called, that the get method returns the
     * same thing. This will also use reflection to set the field if no setter exists (mainly used for user immutable
     * entities but Hibernate normally populates).
     *
     * @throws Exception If an expected error occurs.
     */
    @Test
    public void testGettersAndSetters() throws Exception {
        /* Sort items for consistent test runs. */
        final SortedMap<String, GetterSetterPair> getterSetterMapping = new TreeMap<>();

        final T instance = getInstance();

        for (final Method method : instance.getClass().getMethods()) {
            final String methodName = method.getName();

            if (this.ignoredGetFields.contains(methodName)) {
                continue;
            }

            String objectName;
            if (methodName.startsWith("get") && method.getParameters().length == 0) {
                /* Found the get method. */
                objectName = methodName.substring("get".length());

                GetterSetterPair getterSettingPair = getterSetterMapping.get(objectName);
                if (getterSettingPair == null) {
                    getterSettingPair = new GetterSetterPair();
                    getterSetterMapping.put(objectName, getterSettingPair);
                }
                getterSettingPair.setGetter(method);
            } else if (methodName.startsWith("set") && method.getParameters().length == 1) {
                /* Found the set method. */
                objectName = methodName.substring("set".length());

                GetterSetterPair getterSettingPair = getterSetterMapping.get(objectName);
                if (getterSettingPair == null) {
                    getterSettingPair = new GetterSetterPair();
                    getterSetterMapping.put(objectName, getterSettingPair);
                }
                getterSettingPair.setSetter(method);
            } else if (methodName.startsWith("is") && method.getParameters().length == 0) {
                /* Found the is method, which really is a get method. */
                objectName = methodName.substring("is".length());

                GetterSetterPair getterSettingPair = getterSetterMapping.get(objectName);
                if (getterSettingPair == null) {
                    getterSettingPair = new GetterSetterPair();
                    getterSetterMapping.put(objectName, getterSettingPair);
                }
                getterSettingPair.setGetter(method);
            }
        }

        /*
         * Found all our mappings. Now call the getter and setter or set the field via reflection and call the getting
         * it doesn't have a setter.
         */
        for (final Entry<String, GetterSetterPair> entry : getterSetterMapping.entrySet()) {
            final GetterSetterPair pair = entry.getValue();

            final String objectName = entry.getKey();
            final String fieldName = objectName.substring(0, 1).toLowerCase() + objectName.substring(1);

            if (pair.hasGetterAndSetter()) {
                /* Create an object. */
                final Class<?> parameterType = pair.getSetter().getParameterTypes()[0];
                final Object newObject = createObject(fieldName, parameterType);

                pair.getSetter().invoke(instance, newObject);

                callGetter(fieldName, pair.getGetter(), instance, pair.getGetter().getReturnType(), newObject);
            } else if (pair.getGetter() != null) {
                /*
                 * Object is immutable (no setter but Hibernate or something else sets it via reflection). Use
                 * reflection to set object and verify that same object is returned when calling the getter.
                 */
                final Object newObject = createObject(fieldName, pair.getGetter().getReturnType());
                final Field field = instance.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(instance, newObject);

                callGetter(fieldName, pair.getGetter(), instance, pair.getGetter().getReturnType(), newObject);
            }
        }
    }
}
