package org.tabooproject.fluxon.runtime.reflection.bootstrap;

import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
import org.tabooproject.fluxon.runtime.reflection.resolve.FieldResolver;
import org.tabooproject.fluxon.runtime.reflection.util.PolymorphicInlineCache;

import java.lang.invoke.*;

/**
 * 字段访问的 Bootstrap Method
 * 为 invokedynamic 指令提供字段查找和缓存支持
 */
public class FieldBootstrap {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

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
            tryInstallGuard(callSite, targetClass, specialized, fieldName);
            return specialized.invoke(target);
        }
        // 2. 尝试 getter 方法
        MethodHandle getter = FieldResolver.tryCreateGetterHandle(targetClass, fieldName);
        if (getter != null) {
            tryInstallGuard(callSite, targetClass, getter, fieldName);
            return getter.invoke(target);
        }
        // 3. 降级使用 FieldResolver（不缓存，每次动态查找）
        return ReflectionHelper.getField(target, fieldName);
    }

    /**
     * 尝试安装带类型保护的 MethodHandle 到 callsite
     */
    private static void tryInstallGuard(MutableCallSite callSite, Class<?> targetClass, MethodHandle handle, String fieldName) {
        if (!PolymorphicInlineCache.canAddEntry(callSite)) {
            return;
        }
        MethodHandle guarded = createGuardedFieldHandle(targetClass, handle, createFieldFallback(callSite, fieldName), callSite.type());
        if (guarded != null) {
            PolymorphicInlineCache.incrementDepth(callSite);
            callSite.setTarget(guarded);
            MutableCallSite.syncAll(new MutableCallSite[]{callSite});
        }
    }

    /**
     * 创建带类型保护的字段访问 MethodHandle
     * @return 成功返回 guarded MethodHandle，失败返回 null
     */
    private static MethodHandle createGuardedFieldHandle(Class<?> targetClass, MethodHandle specialized, MethodHandle fallback, MethodType callSiteType) {
        try {
            // 创建类型检查: target.getClass() == targetClass
            MethodHandle typeCheck = LOOKUP.findStatic(
                FieldBootstrap.class,
                "checkReceiverType",
                MethodType.methodType(boolean.class, Class.class, Object.class)
            );
            typeCheck = MethodHandles.insertArguments(typeCheck, 0, targetClass);
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
            return null;
        }
    }

    /**
     * 检查 receiver 类型是否精确匹配
     */
    public static boolean checkReceiverType(Class<?> expectedClass, Object target) {
        return target != null && target.getClass() == expectedClass;
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
