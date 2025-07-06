package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class ExtensionMethod {

    public static void init(FluxonRuntime runtime) {
        // 调用方法
        runtime.registerExtensionFunction(Method.class, "invoke", 2, (target, args) -> {
            try {
                Method method = (Method) Objects.requireNonNull(target);
                Object instance = args[0];
                Object[] parameters = (Object[]) args[1];
                method.setAccessible(true);
                return method.invoke(instance, parameters);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke method: " + e.getMessage(), e);
            }
        });
        // 获取方法名
        runtime.registerExtensionFunction(Method.class, "name", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.getName();
        });
        // 获取参数类型
        runtime.registerExtensionFunction(Method.class, "parameterTypes", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.getParameterTypes();
        });
        // 获取返回类型
        runtime.registerExtensionFunction(Method.class, "returnType", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.getReturnType();
        });
        // 获取修饰符
        runtime.registerExtensionFunction(Method.class, "modifiers", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.getModifiers();
        });
        // 设置可访问性
        runtime.registerExtensionFunction(Method.class, "setAccessible", 1, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            boolean accessible = (Boolean) args[0];
            method.setAccessible(accessible);
            return null;
        });
        // 检查是否可访问
        runtime.registerExtensionFunction(Method.class, "isAccessible", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.isAccessible();
        });
        // 检查是否是公共方法
        runtime.registerExtensionFunction(Method.class, "isPublic", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return Modifier.isPublic(method.getModifiers());
        });
        // 检查是否是私有方法
        runtime.registerExtensionFunction(Method.class, "isPrivate", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return Modifier.isPrivate(method.getModifiers());
        });
        // 检查是否是受保护方法
        runtime.registerExtensionFunction(Method.class, "isProtected", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return Modifier.isProtected(method.getModifiers());
        });
        // 检查是否是静态方法
        runtime.registerExtensionFunction(Method.class, "isStatic", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return Modifier.isStatic(method.getModifiers());
        });
        // 检查是否是最终方法
        runtime.registerExtensionFunction(Method.class, "isFinal", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return Modifier.isFinal(method.getModifiers());
        });
        // 检查是否是抽象方法
        runtime.registerExtensionFunction(Method.class, "isAbstract", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return Modifier.isAbstract(method.getModifiers());
        });
        // 获取声明类
        runtime.registerExtensionFunction(Method.class, "declaringClass", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.getDeclaringClass();
        });
        // 获取参数数量
        runtime.registerExtensionFunction(Method.class, "parameterCount", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.getParameterCount();
        });
        // 获取异常类型
        runtime.registerExtensionFunction(Method.class, "exceptionTypes", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.getExceptionTypes();
        });
        // 检查是否是桥接方法
        runtime.registerExtensionFunction(Method.class, "isBridge", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.isBridge();
        });
        // 检查是否是合成方法
        runtime.registerExtensionFunction(Method.class, "isSynthetic", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.isSynthetic();
        });
        // 检查是否是可变参数方法
        runtime.registerExtensionFunction(Method.class, "isVarArgs", 0, (target, args) -> {
            Method method = (Method) Objects.requireNonNull(target);
            return method.isVarArgs();
        });
    }
} 