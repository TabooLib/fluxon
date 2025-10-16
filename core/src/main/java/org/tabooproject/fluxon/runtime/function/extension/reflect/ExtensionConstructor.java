package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExtensionConstructor {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Constructor.class, "fs:reflect")
                // 创建新实例
                .function("newInstance", 1, (context) -> {
                    try {
                        Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                        List<Object> argument = context.getArgumentByType(0, List.class);
                        if (argument != null) {
                            return constructor.newInstance(argument.toArray());
                        } else {
                            return constructor.newInstance();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
                    }
                })
                // 获取参数类型
                .function("parameterTypes", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(constructor.getParameterTypes());
                })
                // 获取修饰符
                .function("modifiers", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return constructor.getModifiers();
                })
                // 设置可访问性
                .function("setAccessible", 1, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    boolean accessible = (Boolean) context.getArgument(0);
                    constructor.setAccessible(accessible);
                    return null;
                })
                // 检查是否可访问
                .function("isAccessible", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return constructor.isAccessible();
                })
                // 检查是否是公共构造器
                .function("isPublic", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return Modifier.isPublic(constructor.getModifiers());
                })
                // 检查是否是私有构造器
                .function("isPrivate", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return Modifier.isPrivate(constructor.getModifiers());
                })
                // 检查是否是受保护构造器
                .function("isProtected", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return Modifier.isProtected(constructor.getModifiers());
                })
                // 获取声明类
                .function("declaringClass", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return constructor.getDeclaringClass();
                })
                // 获取参数数量
                .function("parameterCount", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return constructor.getParameterCount();
                })
                // 获取异常类型
                .function("exceptionTypes", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(constructor.getExceptionTypes());
                })
                // 检查是否是合成构造器
                .function("isSynthetic", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return constructor.isSynthetic();
                })
                // 检查是否是可变参数构造器
                .function("isVarArgs", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return constructor.isVarArgs();
                })
                // 获取构造器名（即类名）
                .function("name", 0, (context) -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    return constructor.getName();
                });
    }
} 