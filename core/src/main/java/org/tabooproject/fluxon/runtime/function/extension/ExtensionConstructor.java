package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

public class ExtensionConstructor {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        // 创建新实例
        runtime.registerExtensionFunction(Constructor.class, "newInstance", 1, (context) -> {
            try {
                Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                Object[] parameters = ((List<Object>) context.getArguments()[0]).toArray();
                constructor.setAccessible(true);
                return constructor.newInstance(parameters);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
            }
        });
        // 获取参数类型
        runtime.registerExtensionFunction(Constructor.class, "parameterTypes", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.getParameterTypes();
        });
        // 获取修饰符
        runtime.registerExtensionFunction(Constructor.class, "modifiers", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.getModifiers();
        });
        // 设置可访问性
        runtime.registerExtensionFunction(Constructor.class, "setAccessible", 1, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            boolean accessible = (Boolean) context.getArguments()[0];
            constructor.setAccessible(accessible);
            return null;
        });
        // 检查是否可访问
        runtime.registerExtensionFunction(Constructor.class, "isAccessible", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.isAccessible();
        });
        // 检查是否是公共构造器
        runtime.registerExtensionFunction(Constructor.class, "isPublic", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return Modifier.isPublic(constructor.getModifiers());
        });
        // 检查是否是私有构造器
        runtime.registerExtensionFunction(Constructor.class, "isPrivate", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return Modifier.isPrivate(constructor.getModifiers());
        });
        // 检查是否是受保护构造器
        runtime.registerExtensionFunction(Constructor.class, "isProtected", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return Modifier.isProtected(constructor.getModifiers());
        });
        // 获取声明类
        runtime.registerExtensionFunction(Constructor.class, "declaringClass", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.getDeclaringClass();
        });
        // 获取参数数量
        runtime.registerExtensionFunction(Constructor.class, "parameterCount", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.getParameterCount();
        });
        // 获取异常类型
        runtime.registerExtensionFunction(Constructor.class, "exceptionTypes", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.getExceptionTypes();
        });
        // 检查是否是合成构造器
        runtime.registerExtensionFunction(Constructor.class, "isSynthetic", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.isSynthetic();
        });
        // 检查是否是可变参数构造器
        runtime.registerExtensionFunction(Constructor.class, "isVarArgs", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.isVarArgs();
        });
        // 获取构造器名（即类名）
        runtime.registerExtensionFunction(Constructor.class, "name", 0, (context) -> {
            Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
            return constructor.getName();
        });
    }
} 