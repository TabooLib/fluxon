package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExtensionClass {

    @SuppressWarnings({"DuplicatedCode", "unchecked"})
    public static void init(FluxonRuntime runtime) {
        // 获取类名
        runtime.registerExtensionFunction(Class.class, "name", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getName();
        });
        // 获取简单类名
        runtime.registerExtensionFunction(Class.class, "simpleName", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getSimpleName();
        });
        // 获取规范名
        runtime.registerExtensionFunction(Class.class, "canonicalName", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getCanonicalName();
        });
        // 获取类型名
        runtime.registerExtensionFunction(Class.class, "typeName", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getTypeName();
        });
        // 检查是否是接口
        runtime.registerExtensionFunction(Class.class, "isInterface", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.isInterface();
        });
        // 检查是否是数组
        runtime.registerExtensionFunction(Class.class, "isArray", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.isArray();
        });
        // 检查是否是原始类型
        runtime.registerExtensionFunction(Class.class, "isPrimitive", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.isPrimitive();
        });
        // 检查是否是注解
        runtime.registerExtensionFunction(Class.class, "isAnnotation", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.isAnnotation();
        });
        // 检查是否是枚举
        runtime.registerExtensionFunction(Class.class, "isEnum", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.isEnum();
        });
        // 检查是否可以从某个类赋值
        runtime.registerExtensionFunction(Class.class, "isAssignableFrom", 1, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            Class<?> other = (Class<?>) args[0];
            return clazz.isAssignableFrom(other);
        });
        // 检查是否是某个对象的实例
        runtime.registerExtensionFunction(Class.class, "isInstance", 1, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.isInstance(args[0]);
        });
        // 获取父类
        runtime.registerExtensionFunction(Class.class, "superclass", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getSuperclass();
        });
        // 获取接口
        runtime.registerExtensionFunction(Class.class, "interfaces", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getInterfaces();
        });
        // 获取包
        runtime.registerExtensionFunction(Class.class, "package", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getPackage();
        });
        // 获取包名
        runtime.registerExtensionFunction(Class.class, "packageName", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getPackage().getName();
        });
        // 获取类加载器
        runtime.registerExtensionFunction(Class.class, "classLoader", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getClassLoader();
        });
        // 获取修饰符
        runtime.registerExtensionFunction(Class.class, "modifiers", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getModifiers();
        });
        // 检查是否是公共类
        runtime.registerExtensionFunction(Class.class, "isPublic", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return Modifier.isPublic(clazz.getModifiers());
        });
        // 检查是否是私有类
        runtime.registerExtensionFunction(Class.class, "isPrivate", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return Modifier.isPrivate(clazz.getModifiers());
        });
        // 检查是否是受保护类
        runtime.registerExtensionFunction(Class.class, "isProtected", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return Modifier.isProtected(clazz.getModifiers());
        });
        // 检查是否是抽象类
        runtime.registerExtensionFunction(Class.class, "isAbstract", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return Modifier.isAbstract(clazz.getModifiers());
        });
        // 检查是否是最终类
        runtime.registerExtensionFunction(Class.class, "isFinal", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return Modifier.isFinal(clazz.getModifiers());
        });
        // 检查是否是静态类
        runtime.registerExtensionFunction(Class.class, "isStatic", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return Modifier.isStatic(clazz.getModifiers());
        });
        // 获取组件类型（数组用）
        runtime.registerExtensionFunction(Class.class, "componentType", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getComponentType();
        });
        // 强制类型转换
        runtime.registerExtensionFunction(Class.class, "cast", 1, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.cast(args[0]);
        });
        // 创建实例（无参构造器）
        runtime.registerExtensionFunction(Class.class, "newInstance", 0, (target, args) -> {
            try {
                Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
            }
        });
        // 获取所有构造器
        runtime.registerExtensionFunction(Class.class, "constructors", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getConstructors();
        });
        // 获取所有声明的构造器
        runtime.registerExtensionFunction(Class.class, "declaredConstructors", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getDeclaredConstructors();
        });
        // 获取所有方法
        runtime.registerExtensionFunction(Class.class, "methods", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getMethods();
        });
        // 获取所有声明的方法
        runtime.registerExtensionFunction(Class.class, "declaredMethods", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getDeclaredMethods();
        });
        // 获取所有字段
        runtime.registerExtensionFunction(Class.class, "fields", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getFields();
        });
        // 获取所有声明的字段
        runtime.registerExtensionFunction(Class.class, "declaredFields", 0, (target, args) -> {
            Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
            return clazz.getDeclaredFields();
        });
        // 获取特定名称的方法（可能有多个重载）
        runtime.registerExtensionFunction(Class.class, "getMethod", 1, (target, args) -> {
            try {
                Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
                String methodName = args[0].toString();
                List<Method> methods = new ArrayList<>();
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        methods.add(method);
                    }
                }
                return methods.size() == 1 ? methods.get(0) : methods.toArray(new Method[0]);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get method: " + e.getMessage(), e);
            }
        });
        // 获取特定名称的声明方法
        runtime.registerExtensionFunction(Class.class, "getDeclaredMethod", 1, (target, args) -> {
            try {
                Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
                String methodName = args[0].toString();
                List<Method> methods = new ArrayList<>();
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        methods.add(method);
                    }
                }
                return methods.size() == 1 ? methods.get(0) : methods.toArray(new Method[0]);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get declared method: " + e.getMessage(), e);
            }
        });
        // 获取特定名称的字段
        runtime.registerExtensionFunction(Class.class, "getField", 1, (target, args) -> {
            try {
                Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
                String fieldName = args[0].toString();
                return clazz.getField(fieldName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get field: " + e.getMessage(), e);
            }
        });
        // 获取特定名称的声明字段
        runtime.registerExtensionFunction(Class.class, "getDeclaredField", 1, (target, args) -> {
            try {
                Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
                String fieldName = args[0].toString();
                return clazz.getDeclaredField(fieldName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get declared field: " + e.getMessage(), e);
            }
        });
        // 获取特定参数类型的构造器
        runtime.registerExtensionFunction(Class.class, "getConstructor", 1, (target, args) -> {
            try {
                Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
                List<Object> paramTypes = (List<Object>) args[0];
                Class<?>[] paramClasses = new Class<?>[paramTypes.size()];
                for (int i = 0; i < paramTypes.size(); i++) {
                    if (paramTypes.get(i) instanceof Class) {
                        paramClasses[i] = (Class<?>) paramTypes.get(i);
                    } else {
                        paramClasses[i] = paramTypes.get(i).getClass();
                    }
                }
                return clazz.getConstructor(paramClasses);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get constructor: " + e.getMessage(), e);
            }
        });
        // 获取特定参数类型的声明构造器
        runtime.registerExtensionFunction(Class.class, "getDeclaredConstructor", 1, (target, args) -> {
            try {
                Class<?> clazz = (Class<?>) Objects.requireNonNull(target);
                List<Object> paramTypes = (List<Object>) args[0];
                Class<?>[] paramClasses = new Class<?>[paramTypes.size()];
                for (int i = 0; i < paramTypes.size(); i++) {
                    if (paramTypes.get(i) instanceof Class) {
                        paramClasses[i] = (Class<?>) paramTypes.get(i);
                    } else {
                        paramClasses[i] = paramTypes.get(i).getClass();
                    }
                }
                return clazz.getDeclaredConstructor(paramClasses);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get declared constructor: " + e.getMessage(), e);
            }
        });
    }
}
