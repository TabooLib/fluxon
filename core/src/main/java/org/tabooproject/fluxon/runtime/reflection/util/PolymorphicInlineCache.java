package org.tabooproject.fluxon.runtime.reflection.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polymorphic Inline Cache (PIC) 管理器
 * 管理 CallSite 的 PIC 深度和类型检查 Guard
 */
public final class PolymorphicInlineCache {

    private PolymorphicInlineCache() {}

    /**
     * PIC 最大缓存深度
     * 超过此深度后不再添加新的缓存条目，始终走 fallback
     */
    public static final int MAX_PIC_DEPTH = 8;

    /**
     * 每个 CallSite 的 PIC 深度计数器
     */
    private static final ConcurrentHashMap<MutableCallSite, Integer> PIC_DEPTH_MAP = new ConcurrentHashMap<>();

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    // ==================== PIC 深度管理 ====================

    /**
     * 获取指定 CallSite 的 PIC 深度
     */
    public static int getDepth(MutableCallSite callSite) {
        return PIC_DEPTH_MAP.getOrDefault(callSite, 0);
    }

    /**
     * 增加 CallSite 的 PIC 深度
     */
    public static void incrementDepth(MutableCallSite callSite) {
        PIC_DEPTH_MAP.compute(callSite, (k, v) -> v == null ? 1 : v + 1);
    }

    /**
     * 检查是否可以添加新的 PIC 条目
     */
    public static boolean canAddEntry(MutableCallSite callSite) {
        return getDepth(callSite) < MAX_PIC_DEPTH;
    }

    /**
     * 清除所有 PIC 缓存
     */
    public static void clear() {
        PIC_DEPTH_MAP.clear();
    }

    /**
     * 获取 PIC 缓存大小
     */
    public static int size() {
        return PIC_DEPTH_MAP.size();
    }

    // ==================== 方法调用 PIC Guard ====================

    /**
     * 创建 PIC 方法调用条目
     */
    public static MethodHandle createMethodEntry(
            Class<?> targetClass,
            Class<?>[] argTypes,
            MethodHandle specialized,
            MethodHandle fallback,
            MethodType callSiteType) {
        try {
            MethodHandle guard = createFullTypeGuard(targetClass, argTypes);
            MethodHandle adapted = specialized.asType(callSiteType);
            MethodHandle adaptedFallback = fallback.asType(callSiteType);
            return MethodHandles.guardWithTest(guard, adapted, adaptedFallback);
        } catch (Throwable e) {
            return fallback;
        }
    }

    /**
     * 创建完整类型检查 guard
     * 签名: (Object, Object[]) -> boolean
     */
    private static MethodHandle createFullTypeGuard(Class<?> targetClass, Class<?>[] argTypes) throws Throwable {
        MethodHandle checker = LOOKUP.findStatic(
            PolymorphicInlineCache.class,
            "checkFullTypeSignature",
            MethodType.methodType(boolean.class, Class.class, Class[].class, Object.class, Object[].class)
        );
        return MethodHandles.insertArguments(checker, 0, targetClass, argTypes);
    }

    /**
     * 检查完整类型签名（receiver + 所有参数）
     */
    public static boolean checkFullTypeSignature(Class<?> expectedTarget, Class<?>[] expectedArgs, Object target, Object[] args) {
        if (target == null || target.getClass() != expectedTarget) {
            return false;
        }
        if (args.length != expectedArgs.length) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            Class<?> expectedArg = expectedArgs[i];
            Object actualArg = args[i];
            if (expectedArg == Void.class) {
                if (actualArg != null) return false;
            } else {
                if (actualArg == null) return false;
                if (actualArg.getClass() != expectedArg) return false;
            }
        }
        return true;
    }

    // ==================== 构造函数 PIC Guard ====================

    /**
     * 创建 PIC 构造函数调用条目
     */
    public static MethodHandle createConstructorEntry(
            String className,
            Class<?>[] argTypes,
            MethodHandle specialized,
            MethodHandle fallback,
            MethodType callSiteType) {
        try {
            MethodHandle guard = createConstructorTypeGuard(className, argTypes);
            MethodHandle adapted = specialized.asType(callSiteType);
            MethodHandle adaptedFallback = fallback.asType(callSiteType);
            return MethodHandles.guardWithTest(guard, adapted, adaptedFallback);
        } catch (Throwable e) {
            return fallback;
        }
    }

    /**
     * 创建构造函数类型检查 guard
     * 签名: (String, Object[]) -> boolean
     */
    private static MethodHandle createConstructorTypeGuard(String className, Class<?>[] argTypes) throws Throwable {
        MethodHandle checker = LOOKUP.findStatic(
            PolymorphicInlineCache.class,
            "checkConstructorTypeSignature",
            MethodType.methodType(boolean.class, String.class, Class[].class, String.class, Object[].class)
        );
        return MethodHandles.insertArguments(checker, 0, className, argTypes);
    }

    /**
     * 检查构造函数类型签名（类名 + 所有参数）
     */
    public static boolean checkConstructorTypeSignature(String expectedClassName, Class<?>[] expectedArgs, String className, Object[] args) {
        if (!expectedClassName.equals(className)) {
            return false;
        }
        if (args.length != expectedArgs.length) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            Class<?> expectedArg = expectedArgs[i];
            if (expectedArg == Void.class) {
                if (args[i] != null) return false;
            } else {
                if (args[i] == null || args[i].getClass() != expectedArg) return false;
            }
        }
        return true;
    }
}
