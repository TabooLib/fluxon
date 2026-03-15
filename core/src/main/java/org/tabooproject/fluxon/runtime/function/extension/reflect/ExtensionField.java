package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.UnsafeAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class ExtensionField {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionField.class);
    }

    @FluxonFunction(value = "get", target = Field.class, namespace = "fs:reflect")
    public static Object get(Field field, Object instance) {
        try {
            return UnsafeAccess.get(instance, Objects.requireNonNull(field));
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get field value: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "set", target = Field.class, namespace = "fs:reflect")
    public static void set(Field field, Object instance, Object value) {
        try {
            UnsafeAccess.put(instance, Objects.requireNonNull(field), value);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to set field value: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "name", target = Field.class, namespace = "fs:reflect")
    public static String name(Field field) {
        return Objects.requireNonNull(field).getName();
    }

    @FluxonFunction(value = "type", target = Field.class, namespace = "fs:reflect")
    public static Class<?> type(Field field) {
        return Objects.requireNonNull(field).getType();
    }

    @FluxonFunction(value = "modifiers", target = Field.class, namespace = "fs:reflect")
    public static int modifiers(Field field) {
        return Objects.requireNonNull(field).getModifiers();
    }

    @FluxonFunction(value = "setAccessible", target = Field.class, namespace = "fs:reflect")
    public static void setAccessible(Field field, boolean accessible) {
        Objects.requireNonNull(field).setAccessible(accessible);
    }

    @FluxonFunction(value = "isAccessible", target = Field.class, namespace = "fs:reflect")
    public static boolean isAccessible(Field field) {
        return Objects.requireNonNull(field).isAccessible();
    }

    @FluxonFunction(value = "isPublic", target = Field.class, namespace = "fs:reflect")
    public static boolean isPublic(Field field) {
        return Modifier.isPublic(Objects.requireNonNull(field).getModifiers());
    }

    @FluxonFunction(value = "isPrivate", target = Field.class, namespace = "fs:reflect")
    public static boolean isPrivate(Field field) {
        return Modifier.isPrivate(Objects.requireNonNull(field).getModifiers());
    }

    @FluxonFunction(value = "isProtected", target = Field.class, namespace = "fs:reflect")
    public static boolean isProtected(Field field) {
        return Modifier.isProtected(Objects.requireNonNull(field).getModifiers());
    }

    @FluxonFunction(value = "isStatic", target = Field.class, namespace = "fs:reflect")
    public static boolean isStatic(Field field) {
        return Modifier.isStatic(Objects.requireNonNull(field).getModifiers());
    }

    @FluxonFunction(value = "isFinal", target = Field.class, namespace = "fs:reflect")
    public static boolean isFinal(Field field) {
        return Modifier.isFinal(Objects.requireNonNull(field).getModifiers());
    }

    @FluxonFunction(value = "isVolatile", target = Field.class, namespace = "fs:reflect")
    public static boolean isVolatile(Field field) {
        return Modifier.isVolatile(Objects.requireNonNull(field).getModifiers());
    }

    @FluxonFunction(value = "isTransient", target = Field.class, namespace = "fs:reflect")
    public static boolean isTransient(Field field) {
        return Modifier.isTransient(Objects.requireNonNull(field).getModifiers());
    }

    @FluxonFunction(value = "declaringClass", target = Field.class, namespace = "fs:reflect")
    public static Class<?> declaringClass(Field field) {
        return Objects.requireNonNull(field).getDeclaringClass();
    }

    @FluxonFunction(value = "isSynthetic", target = Field.class, namespace = "fs:reflect")
    public static boolean isSynthetic(Field field) {
        return Objects.requireNonNull(field).isSynthetic();
    }

    @FluxonFunction(value = "isEnumConstant", target = Field.class, namespace = "fs:reflect")
    public static boolean isEnumConstant(Field field) {
        return Objects.requireNonNull(field).isEnumConstant();
    }
}
