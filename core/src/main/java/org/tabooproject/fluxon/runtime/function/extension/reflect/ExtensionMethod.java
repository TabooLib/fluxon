package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class ExtensionMethod {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Method.class, "fs:reflect")
                // 调用方法
                .function("invoke", 2, (context) -> {
                    try {
                        Method method = Objects.requireNonNull(context.getTarget());
                        method.setAccessible(true);
                        Object instance = context.getRef(0);
                        List<Object> argument = (List<Object>) context.getRef(1);
                        if (argument != null) {
                            context.setReturnRef(method.invoke(instance, argument.toArray()));
                        } else {
                            context.setReturnRef(method.invoke(instance));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke method: " + e.getMessage(), e);
                    }
                })
                // 获取方法名
                .function("name", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.getName());
                })
                // 获取参数类型
                .function("parameterTypes", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(method.getParameterTypes()));
                })
                // 获取返回类型
                .function("returnType", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.getReturnType());
                })
                // 获取修饰符
                .function("modifiers", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.getModifiers());
                })
                // 设置可访问性
                .function("setAccessible", 1, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    boolean accessible = (Boolean) context.getArgBoxed(0);
                    method.setAccessible(accessible);
                })
                // 检查是否可访问
                .function("isAccessible", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.isAccessible());
                })
                // 检查是否是公共方法
                .function("isPublic", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Modifier.isPublic(method.getModifiers()));
                })
                // 检查是否是私有方法
                .function("isPrivate", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Modifier.isPrivate(method.getModifiers()));
                })
                // 检查是否是受保护方法
                .function("isProtected", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Modifier.isProtected(method.getModifiers()));
                })
                // 检查是否是静态方法
                .function("isStatic", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Modifier.isStatic(method.getModifiers()));
                })
                // 检查是否是最终方法
                .function("isFinal", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Modifier.isFinal(method.getModifiers()));
                })
                // 检查是否是抽象方法
                .function("isAbstract", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Modifier.isAbstract(method.getModifiers()));
                })
                // 获取声明类
                .function("declaringClass", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.getDeclaringClass());
                })
                // 获取参数数量
                .function("parameterCount", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.getParameterCount());
                })
                // 获取异常类型
                .function("exceptionTypes", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(method.getExceptionTypes()));
                })
                // 检查是否是桥接方法
                .function("isBridge", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.isBridge());
                })
                // 检查是否是合成方法
                .function("isSynthetic", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.isSynthetic());
                })
                // 检查是否是可变参数方法
                .function("isVarArgs", 0, (context) -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.isVarArgs());
                });
    }
}
