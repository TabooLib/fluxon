package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

@SuppressWarnings("deprecation")
public class ExtensionMethod {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Method.class, "fs:reflect")
                .function("invoke", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), context -> {
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
                .function("name", returns(Type.STRING).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.getName());
                })
                .function("parameterTypes", returns(Type.LIST).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(method.getParameterTypes()));
                })
                .function("returnType", returns(Type.CLASS).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.getReturnType());
                })
                .function("modifiers", returns(Type.I).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(method.getModifiers());
                })
                .function("setAccessible", returns(Type.VOID).params(Type.Z), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    boolean accessible = context.getBool(0);
                    method.setAccessible(accessible);
                })
                .function("isAccessible", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(method.isAccessible());
                })
                .function("isPublic", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isPublic(method.getModifiers()));
                })
                .function("isPrivate", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isPrivate(method.getModifiers()));
                })
                .function("isProtected", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isProtected(method.getModifiers()));
                })
                .function("isStatic", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isStatic(method.getModifiers()));
                })
                .function("isFinal", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isFinal(method.getModifiers()));
                })
                .function("isAbstract", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isAbstract(method.getModifiers()));
                })
                .function("declaringClass", returns(Type.CLASS).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(method.getDeclaringClass());
                })
                .function("parameterCount", returns(Type.I).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(method.getParameterCount());
                })
                .function("exceptionTypes", returns(Type.LIST).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(method.getExceptionTypes()));
                })
                .function("isBridge", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(method.isBridge());
                })
                .function("isSynthetic", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(method.isSynthetic());
                })
                .function("isVarArgs", returns(Type.Z).noParams(), context -> {
                    Method method = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(method.isVarArgs());
                });
    }
}
