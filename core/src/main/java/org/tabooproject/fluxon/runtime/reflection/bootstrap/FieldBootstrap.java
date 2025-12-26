package org.tabooproject.fluxon.runtime.reflection.bootstrap;

import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
import org.tabooproject.fluxon.runtime.reflection.resolve.FieldResolver;

import java.lang.invoke.*;

/**
 * 字段访问的 Bootstrap Method
 * 为 invokedynamic 指令提供字段查找和缓存支持
 */
public class FieldBootstrap {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    // 类型检查的 MethodHandle: boolean isInstance(Class<?>, Object)
    private static final MethodHandle IS_INSTANCE;

    static {
        try {
            IS_INSTANCE = LOOKUP.findVirtual(Class.class, "isInstance", MethodType.methodType(boolean.class, Object.class));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Bootstrap Method for invokedynamic (字段访问)
     */
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
        MutableCallSite callSite = new MutableCallSite(type);
        MethodHandle fallback = lookup.findStatic(
                FieldBootstrap.class,
                "lookupAndGetField",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object.class)
        );
        MethodHandle target = fallback
                .bindTo(callSite)
                .bindTo(fieldName)
                .asType(type);
        callSite.setTarget(target);
        return callSite;
    }

    /**
     * 首次字段访问时的查找逻辑
     * 优化：尝试创建类型特化的直接字段访问
     */
    public static Object lookupAndGetField(MutableCallSite callSite, String fieldName, Object target) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot access field '" + fieldName + "' on null object");
        }
        Class<?> targetClass = target.getClass();
        // 1. 尝试创建类型特化的直接字段访问
        MethodHandle specialized = FieldResolver.tryCreateSpecializedFieldHandle(targetClass, fieldName);
        if (specialized != null) {
            // 创建带类型保护的 MethodHandle
            MethodHandle guarded = createGuardedFieldHandle(
                targetClass, specialized,
                createFieldFallback(callSite, fieldName),
                callSite.type()
            );
            callSite.setTarget(guarded);
            return specialized.invoke(target);
        }
        // 2. 尝试 getter 方法
        MethodHandle getter = FieldResolver.tryCreateGetterHandle(targetClass, fieldName);
        if (getter != null) {
            MethodHandle guarded = createGuardedFieldHandle(
                targetClass, getter,
                createFieldFallback(callSite, fieldName),
                callSite.type()
            );
            callSite.setTarget(guarded);
            return getter.invoke(target);
        }
        // 3. 降级使用 FieldResolver（不缓存，每次动态查找）
        return ReflectionHelper.getField(target, fieldName);
    }

    /**
     * 创建带类型保护的字段访问 MethodHandle
     */
    private static MethodHandle createGuardedFieldHandle(Class<?> targetClass, MethodHandle specialized, MethodHandle fallback, MethodType callSiteType) {
        try {
            // 创建类型检查: target instanceof TargetClass
            MethodHandle typeCheck = IS_INSTANCE.bindTo(targetClass);
            // 适配 specialized 的返回类型（可能是原始类型，需要装箱）
            MethodHandle adapted = specialized;
            if (adapted.type().returnType() != callSiteType.returnType()) {
                adapted = adapted.asType(adapted.type().changeReturnType(callSiteType.returnType()));
            }
            // 适配参数类型
            if (adapted.type().parameterType(0) != callSiteType.parameterType(0)) {
                adapted = adapted.asType(callSiteType);
            }
            MethodHandle adaptedFallback = fallback.asType(callSiteType);
            return MethodHandles.guardWithTest(typeCheck, adapted, adaptedFallback);
        } catch (Exception e) {
            return fallback.asType(callSiteType);
        }
    }

    /**
     * 创建字段访问的 fallback MethodHandle
     */
    private static MethodHandle createFieldFallback(MutableCallSite callSite, String fieldName) {
        try {
            MethodHandle fallback = LOOKUP.findStatic(
                FieldBootstrap.class,
                "lookupAndGetField",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object.class)
            );
            return fallback.bindTo(callSite).bindTo(fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
