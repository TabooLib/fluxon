package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class ExtensionMethod {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionMethod.class);
    }

    @FluxonFunction(value = "name", target = Method.class, namespace = "fs:reflect")
    public static String name(Method method) {
        return Objects.requireNonNull(method).getName();
    }

    @FluxonFunction(value = "parameterTypes", target = Method.class, namespace = "fs:reflect")
    public static Object parameterTypes(Method method) {
        return Arrays.asList(Objects.requireNonNull(method).getParameterTypes());
    }

    @FluxonFunction(value = "returnType", target = Method.class, namespace = "fs:reflect")
    public static Class<?> returnType(Method method) {
        return Objects.requireNonNull(method).getReturnType();
    }

    @FluxonFunction(value = "modifiers", target = Method.class, namespace = "fs:reflect")
    public static int modifiers(Method method) {
        return Objects.requireNonNull(method).getModifiers();
    }

    @FluxonFunction(value = "setAccessible", target = Method.class, namespace = "fs:reflect")
    public static void setAccessible(Method method, boolean accessible) {
        Objects.requireNonNull(method).setAccessible(accessible);
    }

    @FluxonFunction(value = "isAccessible", target = Method.class, namespace = "fs:reflect")
    public static boolean isAccessible(Method method) {
        return Objects.requireNonNull(method).isAccessible();
    }

    @FluxonFunction(value = "isPublic", target = Method.class, namespace = "fs:reflect")
    public static boolean isPublic(Method method) {
        return Modifier.isPublic(Objects.requireNonNull(method).getModifiers());
    }

    @FluxonFunction(value = "isPrivate", target = Method.class, namespace = "fs:reflect")
    public static boolean isPrivate(Method method) {
        return Modifier.isPrivate(Objects.requireNonNull(method).getModifiers());
    }

    @FluxonFunction(value = "isProtected", target = Method.class, namespace = "fs:reflect")
    public static boolean isProtected(Method method) {
        return Modifier.isProtected(Objects.requireNonNull(method).getModifiers());
    }

    @FluxonFunction(value = "isStatic", target = Method.class, namespace = "fs:reflect")
    public static boolean isStatic(Method method) {
        return Modifier.isStatic(Objects.requireNonNull(method).getModifiers());
    }

    @FluxonFunction(value = "isFinal", target = Method.class, namespace = "fs:reflect")
    public static boolean isFinal(Method method) {
        return Modifier.isFinal(Objects.requireNonNull(method).getModifiers());
    }

    @FluxonFunction(value = "isAbstract", target = Method.class, namespace = "fs:reflect")
    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(Objects.requireNonNull(method).getModifiers());
    }

    @FluxonFunction(value = "declaringClass", target = Method.class, namespace = "fs:reflect")
    public static Class<?> declaringClass(Method method) {
        return Objects.requireNonNull(method).getDeclaringClass();
    }

    @FluxonFunction(value = "parameterCount", target = Method.class, namespace = "fs:reflect")
    public static int parameterCount(Method method) {
        return Objects.requireNonNull(method).getParameterCount();
    }

    @FluxonFunction(value = "exceptionTypes", target = Method.class, namespace = "fs:reflect")
    public static Object exceptionTypes(Method method) {
        return Arrays.asList(Objects.requireNonNull(method).getExceptionTypes());
    }

    @FluxonFunction(value = "isBridge", target = Method.class, namespace = "fs:reflect")
    public static boolean isBridge(Method method) {
        return Objects.requireNonNull(method).isBridge();
    }

    @FluxonFunction(value = "isSynthetic", target = Method.class, namespace = "fs:reflect")
    public static boolean isSynthetic(Method method) {
        return Objects.requireNonNull(method).isSynthetic();
    }

    @FluxonFunction(value = "isVarArgs", target = Method.class, namespace = "fs:reflect")
    public static boolean isVarArgs(Method method) {
        return Objects.requireNonNull(method).isVarArgs();
    }

    @SuppressWarnings("unchecked")
    @FluxonFunction(value = "invoke", target = Method.class, namespace = "fs:reflect")
    public static Object invoke(Method method, Object instance, Object argument) {
        try {
            Objects.requireNonNull(method).setAccessible(true);
            if (argument instanceof List) {
                return method.invoke(instance, ((List<Object>) argument).toArray());
            }
            return method.invoke(instance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + e.getMessage(), e);
        }
    }
}
