package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.stdlib.UnsafeAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class ExtensionField {

    public static void init(FluxonRuntime runtime) {
        // 获取字段值
        runtime.registerExtensionFunction(Field.class, "get", 1, (context) -> {
            try {
                Field field = (Field) Objects.requireNonNull(context.getTarget());
                return UnsafeAccess.get(context.getArguments()[0], field);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to get field value: " + e.getMessage(), e);
            }
        });
        // 设置字段值
        runtime.registerExtensionFunction(Field.class, "set", 2, (context) -> {
            try {
                Field field = (Field) Objects.requireNonNull(context.getTarget());
                Object instance = context.getArguments()[0];
                Object value = context.getArguments()[1];
                UnsafeAccess.put(instance, field, value);
                return null;
            } catch (Throwable e) {
                throw new RuntimeException("Failed to set field value: " + e.getMessage(), e);
            }
        });
        // 获取字段名
        runtime.registerExtensionFunction(Field.class, "name", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return field.getName();
        });
        // 获取字段类型
        runtime.registerExtensionFunction(Field.class, "type", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return field.getType();
        });
        // 获取修饰符
        runtime.registerExtensionFunction(Field.class, "modifiers", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return field.getModifiers();
        });
        // 设置可访问性
        runtime.registerExtensionFunction(Field.class, "setAccessible", 1, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            boolean accessible = (Boolean) context.getArguments()[0];
            field.setAccessible(accessible);
            return null;
        });
        // 检查是否可访问
        runtime.registerExtensionFunction(Field.class, "isAccessible", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return field.isAccessible();
        });
        // 检查是否是公共字段
        runtime.registerExtensionFunction(Field.class, "isPublic", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return Modifier.isPublic(field.getModifiers());
        });
        // 检查是否是私有字段
        runtime.registerExtensionFunction(Field.class, "isPrivate", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return Modifier.isPrivate(field.getModifiers());
        });
        // 检查是否是受保护字段
        runtime.registerExtensionFunction(Field.class, "isProtected", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return Modifier.isProtected(field.getModifiers());
        });
        // 检查是否是静态字段
        runtime.registerExtensionFunction(Field.class, "isStatic", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return Modifier.isStatic(field.getModifiers());
        });
        // 检查是否是最终字段
        runtime.registerExtensionFunction(Field.class, "isFinal", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return Modifier.isFinal(field.getModifiers());
        });
        // 检查是否是 volatile 字段
        runtime.registerExtensionFunction(Field.class, "isVolatile", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return Modifier.isVolatile(field.getModifiers());
        });
        // 检查是否是 transient 字段
        runtime.registerExtensionFunction(Field.class, "isTransient", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return Modifier.isTransient(field.getModifiers());
        });
        // 获取声明类
        runtime.registerExtensionFunction(Field.class, "declaringClass", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return field.getDeclaringClass();
        });
        // 检查是否是合成字段
        runtime.registerExtensionFunction(Field.class, "isSynthetic", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return field.isSynthetic();
        });
        // 检查是否是枚举常量
        runtime.registerExtensionFunction(Field.class, "isEnumConstant", 0, (context) -> {
            Field field = (Field) Objects.requireNonNull(context.getTarget());
            return field.isEnumConstant();
        });
    }
} 