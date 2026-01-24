package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

@SuppressWarnings("deprecation")
public class ExtensionConstructor {

    @SuppressWarnings("unchecked")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Constructor.class, "fs:reflect")
                .function("newInstance", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    try {
                        Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                        List<Object> argument = (List<Object>) context.getRef(0);
                        if (argument != null) {
                            context.setReturnRef(constructor.newInstance(argument.toArray()));
                        } else {
                            context.setReturnRef(constructor.newInstance());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance: " + e.getMessage(), e);
                    }
                })
                .function("parameterTypes", returns(Type.OBJECT).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(constructor.getParameterTypes()));
                })
                .function("modifiers", returns(Type.I).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(constructor.getModifiers());
                })
                .function("setAccessible", returns(Type.VOID).params(Type.Z), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    boolean accessible = (Boolean) context.getArgBoxed(0);
                    constructor.setAccessible(accessible);
                })
                .function("isAccessible", returns(Type.Z).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(constructor.isAccessible());
                })
                .function("isPublic", returns(Type.Z).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isPublic(constructor.getModifiers()));
                })
                .function("isPrivate", returns(Type.Z).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isPrivate(constructor.getModifiers()));
                })
                .function("isProtected", returns(Type.Z).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isProtected(constructor.getModifiers()));
                })
                .function("declaringClass", returns(Type.OBJECT).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(constructor.getDeclaringClass());
                })
                .function("parameterCount", returns(Type.I).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(constructor.getParameterCount());
                })
                .function("exceptionTypes", returns(Type.OBJECT).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Arrays.asList(constructor.getExceptionTypes()));
                })
                .function("isSynthetic", returns(Type.Z).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(constructor.isSynthetic());
                })
                .function("isVarArgs", returns(Type.Z).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(constructor.isVarArgs());
                })
                .function("name", returns(Type.OBJECT).noParams(), context -> {
                    Constructor<?> constructor = (Constructor<?>) Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(constructor.getName());
                });
    }
}
