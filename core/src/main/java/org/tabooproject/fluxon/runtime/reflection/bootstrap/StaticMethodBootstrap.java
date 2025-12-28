package org.tabooproject.fluxon.runtime.reflection.bootstrap;

import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
import org.tabooproject.fluxon.runtime.reflection.resolve.MethodResolver;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;
import org.tabooproject.fluxon.runtime.reflection.util.VarargsHandler;
import org.tabooproject.fluxon.runtime.error.MemberNotFoundError;

import java.lang.invoke.*;
import java.lang.reflect.Method;

/**
 * 静态方法调用的 Bootstrap Method
 * 为 invokedynamic 指令提供静态方法查找和缓存支持
 */
public class StaticMethodBootstrap {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    /**
     * Bootstrap Method for static method invocation
     * 签名: (Class, Object[]) -> Object
     */
    public static CallSite bootstrapMethod(MethodHandles.Lookup lookup, String memberName, MethodType type) throws Throwable {
        MutableCallSite callSite = new MutableCallSite(type);
        MethodHandle fallback = lookup.findStatic(
                StaticMethodBootstrap.class,
                "invokeStaticMethod",
                MethodType.methodType(Object.class, String.class, Class.class, Object[].class)
        );
        MethodHandle bound = MethodHandles.insertArguments(fallback, 0, memberName);
        callSite.setTarget(bound.asType(type));
        return callSite;
    }

    /**
     * Bootstrap Method for static field access
     * 签名: (Class) -> Object
     */
    public static CallSite bootstrapField(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
        MutableCallSite callSite = new MutableCallSite(type);
        MethodHandle fallback = lookup.findStatic(
                StaticMethodBootstrap.class,
                "getStaticField",
                MethodType.methodType(Object.class, String.class, Class.class)
        );
        MethodHandle bound = MethodHandles.insertArguments(fallback, 0, fieldName);
        callSite.setTarget(bound.asType(type));
        return callSite;
    }

    /**
     * 静态方法调用的查找逻辑
     */
    public static Object invokeStaticMethod(String memberName, Class<?> targetClass, Object[] args) throws Throwable {
        Class<?>[] argTypes = TypeCompatibility.getArgTypes(args);
        Method staticMethod = MethodResolver.findBestStaticMatch(targetClass, memberName, argTypes);
        if (staticMethod == null) {
            throw new MemberNotFoundError(targetClass, memberName, argTypes);
        }
        return invokeMethod(staticMethod, args);
    }

    /**
     * 静态字段访问的查找逻辑
     */
    public static Object getStaticField(String fieldName, Class<?> targetClass) throws Throwable {
        return ReflectionHelper.getStaticField(targetClass, fieldName);
    }

    /**
     * 调用静态方法
     */
    private static Object invokeMethod(Method method, Object[] args) throws Throwable {
        if (method.isVarArgs()) {
            return VarargsHandler.invokeVarargsMethod(method, null, args);
        }
        try {
            MethodHandle mh = LOOKUP.unreflect(method);
            int argCount = args.length;
            if (argCount == 0) {
                return mh.invoke();
            }
            MethodHandle spread = mh.asSpreader(Object[].class, argCount);
            return spread.invoke(args);
        } catch (IllegalAccessException e) {
            method.setAccessible(true);
            return method.invoke(null, args);
        }
    }
}
