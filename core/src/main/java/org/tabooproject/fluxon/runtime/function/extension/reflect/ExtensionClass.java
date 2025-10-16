package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExtensionClass {

    @SuppressWarnings({"DuplicatedCode", "unchecked"})
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Class.class)
                // 获取类名
                .function("name", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getName();
                })
                // 获取简单类名
                .function("simpleName", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getSimpleName();
                })
                // 获取规范名
                .function("canonicalName", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getCanonicalName();
                })
                // 获取类型名
                .function("typeName", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getTypeName();
                });
        runtime.registerExtension(Class.class, "fs:reflect")
                // 检查是否是接口
                .function("isInterface", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.isInterface();
                })
                // 检查是否是数组
                .function("isArray", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.isArray();
                })
                // 检查是否是原始类型
                .function("isPrimitive", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.isPrimitive();
                })
                // 检查是否是注解
                .function("isAnnotation", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.isAnnotation();
                })
                // 检查是否是枚举
                .function("isEnum", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.isEnum();
                })
                // 检查是否可以从某个类赋值
                .function("isAssignableFrom", 1, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    Class<?> other = (Class<?>) context.getArgument(0);
                    return clazz.isAssignableFrom(other);
                })
                // 检查是否是某个对象的实例
                .function("isInstance", 1, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.isInstance(context.getArgument(0));
                })
                // 获取父类
                .function("superclass", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getSuperclass();
                })
                // 获取接口
                .function("interfaces", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(clazz.getInterfaces());
                })
                // 获取包
                .function("package", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getPackage();
                })
                // 获取包名
                .function("packageName", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getPackage().getName();
                })
                // 获取类加载器
                .function("classLoader", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getClassLoader();
                })
                // 获取修饰符
                .function("modifiers", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getModifiers();
                })
                // 检查是否是公共类
                .function("isPublic", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Modifier.isPublic(clazz.getModifiers());
                })
                // 检查是否是私有类
                .function("isPrivate", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Modifier.isPrivate(clazz.getModifiers());
                })
                // 检查是否是受保护类
                .function("isProtected", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Modifier.isProtected(clazz.getModifiers());
                })
                // 检查是否是抽象类
                .function("isAbstract", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Modifier.isAbstract(clazz.getModifiers());
                })
                // 检查是否是最终类
                .function("isFinal", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Modifier.isFinal(clazz.getModifiers());
                })
                // 检查是否是静态类
                .function("isStatic", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Modifier.isStatic(clazz.getModifiers());
                })
                // 获取组件类型（数组用）
                .function("componentType", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.getComponentType();
                })
                // 强制类型转换
                .function("cast", 1, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return clazz.cast(context.getArgument(0));
                })
                // 创建实例（无参构造器）
                .function("newInstance", 0, (context) -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        Constructor<?> constructor = clazz.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        return constructor.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
                    }
                })
                // 获取所有构造器
                .function("constructors", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(clazz.getConstructors());
                })
                // 获取所有声明的构造器
                .function("declaredConstructors", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(clazz.getDeclaredConstructors());
                })
                // 获取所有方法
                .function("methods", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(clazz.getMethods());
                })
                // 获取所有声明的方法
                .function("declaredMethods", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(clazz.getDeclaredMethods());
                })
                // 获取所有字段
                .function("fields", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(clazz.getFields());
                })
                // 获取所有声明的字段
                .function("declaredFields", 0, (context) -> {
                    Class<?> clazz = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(clazz.getDeclaredFields());
                })
                // 获取特定名称的方法（可能有多个重载）
                .function("method", 1, (context) -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        String methodName = context.getString(0);
                        List<Method> methods = new ArrayList<>();
                        for (Method method : clazz.getMethods()) {
                            if (method.getName().equals(methodName)) {
                                methods.add(method);
                            }
                        }
                        return methods.size() == 1 ? methods.get(0) : methods;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get method: " + e.getMessage(), e);
                    }
                })
                // 获取特定名称的声明方法
                .function("declaredMethod", 1, (context) -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        String methodName = context.getString(0);
                        List<Method> methods = new ArrayList<>();
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.getName().equals(methodName)) {
                                methods.add(method);
                            }
                        }
                        return methods.size() == 1 ? methods.get(0) : methods;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get declared method: " + e.getMessage(), e);
                    }
                })
                // 获取特定名称的字段
                .function("field", 1, (context) -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        String fieldName = context.getString(0);
                        if (fieldName == null) return null;
                        return clazz.getField(fieldName);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get field: " + e.getMessage(), e);
                    }
                })
                // 获取特定名称的声明字段
                .function("declaredField", 1, (context) -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        String fieldName = context.getString(0);
                        if (fieldName == null) return null;
                        return clazz.getDeclaredField(fieldName);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get declared field: " + e.getMessage(), e);
                    }
                })
                // 获取特定参数类型的构造器
                .function("constructor", 1, (context) -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        List<Object> paramTypes = context.getArgumentByType(0, List.class);
                        if (paramTypes == null) {
                            return clazz.getConstructor();
                        }
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
                })
                // 获取特定参数类型的声明构造器
                .function("declaredConstructor", 1, (context) -> {
                    try {
                        Class<?> clazz = Objects.requireNonNull(context.getTarget());
                        List<Object> paramTypes = context.getArgumentByType(0, List.class);
                        if (paramTypes == null) {
                            return clazz.getDeclaredConstructor();
                        }
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
