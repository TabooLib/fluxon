package org.tabooproject.fluxon.runtime.function.extension.reflect;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.UnsafeAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

@SuppressWarnings("deprecation")
public class ExtensionField {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Field.class, "fs:reflect")
                .function("get", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    try {
                        Field field = Objects.requireNonNull(context.getTarget());
                        context.setReturnRef(UnsafeAccess.get(context.getRef(0), field));
                    } catch (Throwable e) {
                        throw new RuntimeException("Failed to get field value: " + e.getMessage(), e);
                    }
                })
                .function("set", returns(Type.VOID).params(Type.OBJECT, Type.OBJECT), context -> {
                    try {
                        Field field = Objects.requireNonNull(context.getTarget());
                        Object instance = context.getRef(0);
                        Object value = context.getRef(1);
                        UnsafeAccess.put(instance, field, value);
                    } catch (Throwable e) {
                        throw new RuntimeException("Failed to set field value: " + e.getMessage(), e);
                    }
                })
                .function("name", returns(Type.STRING).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(field.getName());
                })
                .function("type", returns(Type.CLASS).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(field.getType());
                })
                .function("modifiers", returns(Type.I).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(field.getModifiers());
                })
                .function("setAccessible", returns(Type.VOID).params(Type.Z), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    boolean accessible = context.getBool(0);
                    field.setAccessible(accessible);
                })
                .function("isAccessible", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(field.isAccessible());
                })
                .function("isPublic", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isPublic(field.getModifiers()));
                })
                .function("isPrivate", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isPrivate(field.getModifiers()));
                })
                .function("isProtected", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isProtected(field.getModifiers()));
                })
                .function("isStatic", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isStatic(field.getModifiers()));
                })
                .function("isFinal", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isFinal(field.getModifiers()));
                })
                .function("isVolatile", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isVolatile(field.getModifiers()));
                })
                .function("isTransient", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Modifier.isTransient(field.getModifiers()));
                })
                .function("declaringClass", returns(Type.CLASS).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(field.getDeclaringClass());
                })
                .function("isSynthetic", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(field.isSynthetic());
                })
                .function("isEnumConstant", returns(Type.Z).noParams(), context -> {
                    Field field = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(field.isEnumConstant());
                });
    }
}
