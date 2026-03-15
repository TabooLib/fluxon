package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class ExtensionConstructor {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionConstructor.class);
    }

    @FluxonFunction(value = "parameterTypes", target = Constructor.class, namespace = "fs:reflect")
    public static Object parameterTypes(Constructor<?> constructor) {
        return Arrays.asList(Objects.requireNonNull(constructor).getParameterTypes());
    }

    @FluxonFunction(value = "modifiers", target = Constructor.class, namespace = "fs:reflect")
    public static int modifiers(Constructor<?> constructor) {
        return Objects.requireNonNull(constructor).getModifiers();
    }

    @FluxonFunction(value = "setAccessible", target = Constructor.class, namespace = "fs:reflect")
    public static void setAccessible(Constructor<?> constructor, boolean accessible) {
        Objects.requireNonNull(constructor).setAccessible(accessible);
    }

    @FluxonFunction(value = "isAccessible", target = Constructor.class, namespace = "fs:reflect")
    public static boolean isAccessible(Constructor<?> constructor) {
        return Objects.requireNonNull(constructor).isAccessible();
    }

    @FluxonFunction(value = "isPublic", target = Constructor.class, namespace = "fs:reflect")
    public static boolean isPublic(Constructor<?> constructor) {
        return Modifier.isPublic(Objects.requireNonNull(constructor).getModifiers());
    }

    @FluxonFunction(value = "isPrivate", target = Constructor.class, namespace = "fs:reflect")
    public static boolean isPrivate(Constructor<?> constructor) {
        return Modifier.isPrivate(Objects.requireNonNull(constructor).getModifiers());
    }

    @FluxonFunction(value = "isProtected", target = Constructor.class, namespace = "fs:reflect")
    public static boolean isProtected(Constructor<?> constructor) {
        return Modifier.isProtected(Objects.requireNonNull(constructor).getModifiers());
    }

    @FluxonFunction(value = "declaringClass", target = Constructor.class, namespace = "fs:reflect")
    public static Class<?> declaringClass(Constructor<?> constructor) {
        return Objects.requireNonNull(constructor).getDeclaringClass();
    }

    @FluxonFunction(value = "parameterCount", target = Constructor.class, namespace = "fs:reflect")
    public static int parameterCount(Constructor<?> constructor) {
        return Objects.requireNonNull(constructor).getParameterCount();
    }

    @FluxonFunction(value = "exceptionTypes", target = Constructor.class, namespace = "fs:reflect")
    public static Object exceptionTypes(Constructor<?> constructor) {
        return Arrays.asList(Objects.requireNonNull(constructor).getExceptionTypes());
    }

    @FluxonFunction(value = "isSynthetic", target = Constructor.class, namespace = "fs:reflect")
    public static boolean isSynthetic(Constructor<?> constructor) {
        return Objects.requireNonNull(constructor).isSynthetic();
    }

    @FluxonFunction(value = "isVarArgs", target = Constructor.class, namespace = "fs:reflect")
    public static boolean isVarArgs(Constructor<?> constructor) {
        return Objects.requireNonNull(constructor).isVarArgs();
    }

    @FluxonFunction(value = "name", target = Constructor.class, namespace = "fs:reflect")
    public static String name(Constructor<?> constructor) {
        return Objects.requireNonNull(constructor).getName();
    }

    @FluxonFunction(value = "newInstance", target = Constructor.class, namespace = "fs:reflect")
    public static Object newInstance(Constructor<?> constructor, List<?> argument) {
        try {
            Objects.requireNonNull(constructor);
            if (argument != null) {
                return constructor.newInstance(argument.toArray());
            }
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
        }
    }
}
