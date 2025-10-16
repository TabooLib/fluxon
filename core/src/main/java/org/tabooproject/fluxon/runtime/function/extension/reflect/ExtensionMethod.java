package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExtensionMethod {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Method.class, "fs:reflect")
                // 调用方法
                .function("invoke", 2, (context) -> {
                    try {
                        Method method = Objects.requireNonNull(context.getTarget());
                        method.setAccessible(true);
                        Object instance = context.getArgument(0);
                        List<Object> argument = context.getArgumentByType(1, List.class);
                        if (argument != null) {
                            return method.invoke(instance, argument.toArray());
                        } else {
                            return method.invoke(instance);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke method: " + e.getMessage(), e);
                    }
                })
                // 获取方法名
                .function("name", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.getName();
                })
                // 获取参数类型
                .function("parameterTypes", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(method.getParameterTypes());
                })
                // 获取返回类型
                .function("returnType", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.getReturnType();
                })
                // 获取修饰符
                .function("modifiers", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.getModifiers();
                })
                // 设置可访问性
                .function("setAccessible", 1, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    boolean accessible = context.getBoolean(0);
                    method.setAccessible(accessible);
                    return null;
                })
                // 检查是否可访问
                .function("isAccessible", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.isAccessible();
                })
                // 检查是否是公共方法
                .function("isPublic", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return Modifier.isPublic(method.getModifiers());
                })
                // 检查是否是私有方法
                .function("isPrivate", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return Modifier.isPrivate(method.getModifiers());
                })
                // 检查是否是受保护方法
                .function("isProtected", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return Modifier.isProtected(method.getModifiers());
                })
                // 检查是否是静态方法
                .function("isStatic", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return Modifier.isStatic(method.getModifiers());
                })
                // 检查是否是最终方法
                .function("isFinal", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return Modifier.isFinal(method.getModifiers());
                })
                // 检查是否是抽象方法
                .function("isAbstract", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return Modifier.isAbstract(method.getModifiers());
                })
                // 获取声明类
                .function("declaringClass", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.getDeclaringClass();
                })
                // 获取参数数量
                .function("parameterCount", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.getParameterCount();
                })
                // 获取异常类型
                .function("exceptionTypes", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return Arrays.asList(method.getExceptionTypes());
                })
                // 检查是否是桥接方法
                .function("isBridge", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.isBridge();
                })
                // 检查是否是合成方法
                .function("isSynthetic", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.isSynthetic();
                })
                // 检查是否是可变参数方法
                .function("isVarArgs", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    return method.isVarArgs();
                });
    }
} 