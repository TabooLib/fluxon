package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.UnsafeAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class ExtensionField {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Field.class, "fs:reflect")
                // 获取字段值
                .function("get", 1, (context) -> {
                    try {
                        Field field = Objects.requireNonNull(context.getTarget());
                        return UnsafeAccess.get(context.getArgument(0), field);
                    } catch (Throwable e) {
                        throw new RuntimeException("Failed to get field value: " + e.getMessage(), e);
                    }
                })
                // 设置字段值
                .function("set", 2, (context) -> {
                    try {
                        Field field = Objects.requireNonNull(context.getTarget());
                        Object instance = context.getArgument(0);
                        Object value = context.getArgument(1);
                        UnsafeAccess.put(instance, field, value);
                        return null;
                    } catch (Throwable e) {
                        throw new RuntimeException("Failed to set field value: " + e.getMessage(), e);
                    }
                })
                // 获取字段名
                .function("name", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return field.getName();
                })
                // 获取字段类型
                .function("type", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return field.getType();
                })
                // 获取修饰符
                .function("modifiers", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return field.getModifiers();
                })
                // 设置可访问性
                .function("setAccessible", 1, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    boolean accessible = context.getBoolean(0);
                    field.setAccessible(accessible);
                    return null;
                })
                // 检查是否可访问
                .function("isAccessible", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return field.isAccessible();
                })
                // 检查是否是公共字段
                .function("isPublic", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return Modifier.isPublic(field.getModifiers());
                })
                // 检查是否是私有字段
                .function("isPrivate", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return Modifier.isPrivate(field.getModifiers());
                })
                // 检查是否是受保护字段
                .function("isProtected", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return Modifier.isProtected(field.getModifiers());
                })
                // 检查是否是静态字段
                .function("isStatic", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return Modifier.isStatic(field.getModifiers());
                })
                // 检查是否是最终字段
                .function("isFinal", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return Modifier.isFinal(field.getModifiers());
                })
                // 检查是否是 volatile 字段
                .function("isVolatile", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return Modifier.isVolatile(field.getModifiers());
                })
                // 检查是否是 transient 字段
                .function("isTransient", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return Modifier.isTransient(field.getModifiers());
                })
                // 获取声明类
                .function("declaringClass", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return field.getDeclaringClass();
                })
                // 检查是否是合成字段
                .function("isSynthetic", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return field.isSynthetic();
                })
                // 检查是否是枚举常量
                .function("isEnumConstant", 0, (context) -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    return field.isEnumConstant();
                });
    }
} 