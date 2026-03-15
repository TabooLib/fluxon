package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ExtensionClass {

    @SuppressWarnings({"unchecked"})
    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionClass.class);
    }

    // 无 namespace 的基础属性
    @FluxonFunction(value = "name", target = Class.class)
    public static String name(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getName();
    }

    @FluxonFunction(value = "simpleName", target = Class.class)
    public static String simpleName(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getSimpleName();
    }

    @FluxonFunction(value = "canonicalName", target = Class.class)
    public static String canonicalName(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getCanonicalName();
    }

    @FluxonFunction(value = "typeName", target = Class.class)
    public static String typeName(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getTypeName();
    }

    // fs:reflect namespace 的函数
    @FluxonFunction(value = "isInterface", target = Class.class, namespace = "fs:reflect")
    public static boolean isInterface(Class<?> clazz) {
        return Objects.requireNonNull(clazz).isInterface();
    }

    @FluxonFunction(value = "isArray", target = Class.class, namespace = "fs:reflect")
    public static boolean isArrayClass(Class<?> clazz) {
        return Objects.requireNonNull(clazz).isArray();
    }

    @FluxonFunction(value = "isPrimitive", target = Class.class, namespace = "fs:reflect")
    public static boolean isPrimitive(Class<?> clazz) {
        return Objects.requireNonNull(clazz).isPrimitive();
    }

    @FluxonFunction(value = "isAnnotation", target = Class.class, namespace = "fs:reflect")
    public static boolean isAnnotation(Class<?> clazz) {
        return Objects.requireNonNull(clazz).isAnnotation();
    }

    @FluxonFunction(value = "isEnum", target = Class.class, namespace = "fs:reflect")
    public static boolean isEnum(Class<?> clazz) {
        return Objects.requireNonNull(clazz).isEnum();
    }

    @FluxonFunction(value = "isAssignableFrom", target = Class.class, namespace = "fs:reflect")
    public static boolean isAssignableFrom(Class<?> clazz, Object other) {
        return Objects.requireNonNull(clazz).isAssignableFrom((Class<?>) other);
    }

    @FluxonFunction(value = "isInstance", target = Class.class, namespace = "fs:reflect")
    public static boolean isInstance(Class<?> clazz, Object obj) {
        return Objects.requireNonNull(clazz).isInstance(obj);
    }

    @FluxonFunction(value = "superclass", target = Class.class, namespace = "fs:reflect")
    public static Class<?> superclass(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getSuperclass();
    }

    @FluxonFunction(value = "interfaces", target = Class.class, namespace = "fs:reflect")
    public static Object interfaces(Class<?> clazz) {
        return Arrays.asList(Objects.requireNonNull(clazz).getInterfaces());
    }

    @FluxonFunction(value = "package", target = Class.class, namespace = "fs:reflect")
    public static Package getPackage(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getPackage();
    }

    @FluxonFunction(value = "packageName", target = Class.class, namespace = "fs:reflect")
    public static String packageName(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getPackage().getName();
    }

    @FluxonFunction(value = "classLoader", target = Class.class, namespace = "fs:reflect")
    public static ClassLoader classLoader(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getClassLoader();
    }

    @FluxonFunction(value = "modifiers", target = Class.class, namespace = "fs:reflect")
    public static int modifiers(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getModifiers();
    }

    @FluxonFunction(value = "isPublic", target = Class.class, namespace = "fs:reflect")
    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(Objects.requireNonNull(clazz).getModifiers());
    }

    @FluxonFunction(value = "isPrivate", target = Class.class, namespace = "fs:reflect")
    public static boolean isPrivate(Class<?> clazz) {
        return Modifier.isPrivate(Objects.requireNonNull(clazz).getModifiers());
    }

    @FluxonFunction(value = "isProtected", target = Class.class, namespace = "fs:reflect")
    public static boolean isProtected(Class<?> clazz) {
        return Modifier.isProtected(Objects.requireNonNull(clazz).getModifiers());
    }

    @FluxonFunction(value = "isAbstract", target = Class.class, namespace = "fs:reflect")
    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(Objects.requireNonNull(clazz).getModifiers());
    }

    @FluxonFunction(value = "isFinal", target = Class.class, namespace = "fs:reflect")
    public static boolean isFinalClass(Class<?> clazz) {
        return Modifier.isFinal(Objects.requireNonNull(clazz).getModifiers());
    }

    @FluxonFunction(value = "isStatic", target = Class.class, namespace = "fs:reflect")
    public static boolean isStatic(Class<?> clazz) {
        return Modifier.isStatic(Objects.requireNonNull(clazz).getModifiers());
    }

    @FluxonFunction(value = "componentType", target = Class.class, namespace = "fs:reflect")
    public static Class<?> componentType(Class<?> clazz) {
        return Objects.requireNonNull(clazz).getComponentType();
    }

    @FluxonFunction(value = "cast", target = Class.class, namespace = "fs:reflect")
    public static Object cast(Class<?> clazz, Object obj) {
        return Objects.requireNonNull(clazz).cast(obj);
    }

    @FluxonFunction(value = "newInstance", target = Class.class, namespace = "fs:reflect")
    public static Object newInstance(Class<?> clazz) {
        try {
            Constructor<?> constructor = Objects.requireNonNull(clazz).getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "constructors", target = Class.class, namespace = "fs:reflect")
    public static Object constructors(Class<?> clazz) {
        return Arrays.asList(Objects.requireNonNull(clazz).getConstructors());
    }

    @FluxonFunction(value = "declaredConstructors", target = Class.class, namespace = "fs:reflect")
    public static Object declaredConstructors(Class<?> clazz) {
        return Arrays.asList(Objects.requireNonNull(clazz).getDeclaredConstructors());
    }

    @FluxonFunction(value = "methods", target = Class.class, namespace = "fs:reflect")
    public static Object methods(Class<?> clazz) {
        return Arrays.asList(Objects.requireNonNull(clazz).getMethods());
    }

    @FluxonFunction(value = "declaredMethods", target = Class.class, namespace = "fs:reflect")
    public static Object declaredMethods(Class<?> clazz) {
        return Arrays.asList(Objects.requireNonNull(clazz).getDeclaredMethods());
    }

    @FluxonFunction(value = "fields", target = Class.class, namespace = "fs:reflect")
    public static Object fields(Class<?> clazz) {
        return Arrays.asList(Objects.requireNonNull(clazz).getFields());
    }

    @FluxonFunction(value = "declaredFields", target = Class.class, namespace = "fs:reflect")
    public static Object declaredFields(Class<?> clazz) {
        return Arrays.asList(Objects.requireNonNull(clazz).getDeclaredFields());
    }

    // method/declaredMethod 按名称查找，返回单个或列表
    @FluxonFunction(value = "method", target = Class.class, namespace = "fs:reflect")
    public static Object method(Class<?> clazz, String methodName) {
        try {
            List<Method> methods = new ArrayList<>();
            for (Method m : Objects.requireNonNull(clazz).getMethods()) {
                if (m.getName().equals(methodName)) {
                    methods.add(m);
                }
            }
            return methods.size() == 1 ? methods.get(0) : methods;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get method: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "declaredMethod", target = Class.class, namespace = "fs:reflect")
    public static Object declaredMethod(Class<?> clazz, String methodName) {
        try {
            List<Method> methods = new ArrayList<>();
            for (Method m : Objects.requireNonNull(clazz).getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    methods.add(m);
                }
            }
            return methods.size() == 1 ? methods.get(0) : methods;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get declared method: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "field", target = Class.class, namespace = "fs:reflect")
    public static Object field(Class<?> clazz, String fieldName) {
        try {
            if (fieldName == null) return null;
            return Objects.requireNonNull(clazz).getField(fieldName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "declaredField", target = Class.class, namespace = "fs:reflect")
    public static Object declaredField(Class<?> clazz, String fieldName) {
        try {
            if (fieldName == null) return null;
            return Objects.requireNonNull(clazz).getDeclaredField(fieldName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get declared field: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "constructor", target = Class.class, namespace = "fs:reflect")
    public static Object constructor(Class<?> clazz, List<?> paramTypes) {
        try {
            Objects.requireNonNull(clazz);
            if (paramTypes == null) {
                return clazz.getConstructor();
            }
            Class<?>[] paramClasses = toClassArray(paramTypes);
            return clazz.getConstructor(paramClasses);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get constructor: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "declaredConstructor", target = Class.class, namespace = "fs:reflect")
    public static Object declaredConstructor(Class<?> clazz, List<?> paramTypes) {
        try {
            Objects.requireNonNull(clazz);
            if (paramTypes == null) {
                return clazz.getDeclaredConstructor();
            }
            Class<?>[] paramClasses = toClassArray(paramTypes);
            return clazz.getDeclaredConstructor(paramClasses);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get declared constructor: " + e.getMessage(), e);
        }
    }

    private static Class<?>[] toClassArray(List<?> paramTypes) {
        Class<?>[] paramClasses = new Class<?>[paramTypes.size()];
        for (int i = 0; i < paramTypes.size(); i++) {
            Object item = paramTypes.get(i);
            paramClasses[i] = item instanceof Class ? (Class<?>) item : item.getClass();
        }
        return paramClasses;
    }
}
