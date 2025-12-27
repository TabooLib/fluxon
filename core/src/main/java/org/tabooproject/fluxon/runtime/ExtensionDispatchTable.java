package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扩展函数派发表
 * 用于优化扩展函数解析，通过预计算和分流策略减少运行时开销
 * <p>
 * 分流策略:
 * 1. 单候选快速路径: 直接存储 class/function，只做 == 和 isAssignableFrom 判断
 * 2. 小候选数组路径: 候选数 <= 6 时用数组单趟扫描，同时处理精确匹配和可赋值回退
 * 3. 大候选 Map 路径: 候选数 > 6 时启用 HashMap 精确匹配 + ConcurrentHashMap 缓存
 */
public final class ExtensionDispatchTable {

    /**
     * 缓存标记：未命中时的占位符
     */
    private static final Function NOT_FOUND_SENTINEL = new NativeFunction<>(null, null, ctx -> null);

    /**
     * 启用 Map + 缓存的阈值：候选数量超过此值时才启用
     */
    private static final int MAP_THRESHOLD = 6;

    /** 单候选时的目标类型 */
    private final Class<?> singleClass;
    /** 单候选时的函数 */
    private final Function singleFunction;

    /** 候选类型数组（用于小候选场景） */
    private final Class<?>[] candidateClasses;
    /** 候选函数数组（用于小候选场景） */
    private final Function[] candidateFunctions;

    /** 精确类型匹配表（仅大候选场景） */
    private final Map<Class<?>, Function> exactMatches;
    /** 可赋值匹配缓存（仅大候选场景） */
    private final ConcurrentHashMap<Class<?>, Function> assignableCache;

    /**
     * 构造派发表
     *
     * @param exactMatches 精确匹配表（来自注册）
     * @param candidateClasses 候选类型数组
     * @param candidateFunctions 候选函数数组
     */
    public ExtensionDispatchTable(
            @NotNull Map<Class<?>, Function> exactMatches,
            @NotNull Class<?>[] candidateClasses,
            @NotNull Function[] candidateFunctions) {
        int count = candidateClasses.length;

        if (count == 0) {
            // 无候选
            this.singleClass = null;
            this.singleFunction = null;
            this.candidateClasses = null;
            this.candidateFunctions = null;
            this.exactMatches = null;
            this.assignableCache = null;
        } else if (count == 1) {
            // 单候选快速路径
            this.singleClass = candidateClasses[0];
            this.singleFunction = candidateFunctions[0];
            this.candidateClasses = null;
            this.candidateFunctions = null;
            this.exactMatches = null;
            this.assignableCache = null;
        } else if (count <= MAP_THRESHOLD) {
            // 小候选数组路径
            this.singleClass = null;
            this.singleFunction = null;
            this.candidateClasses = candidateClasses;
            this.candidateFunctions = candidateFunctions;
            this.exactMatches = null;
            this.assignableCache = null;
        } else {
            // 大候选 Map 路径
            this.singleClass = null;
            this.singleFunction = null;
            this.candidateClasses = candidateClasses;
            this.candidateFunctions = candidateFunctions;
            this.exactMatches = exactMatches;
            this.assignableCache = new ConcurrentHashMap<>();
        }
    }

    /**
     * 解析目标类型对应的扩展函数
     *
     * @param targetClass 目标对象的类型
     * @return 匹配的函数，如果没有找到则返回 null
     */
    @Nullable
    public Function resolve(@NotNull Class<?> targetClass) {
        // 单候选快速路径
        if (singleClass != null) {
            if (singleClass == targetClass || singleClass.isAssignableFrom(targetClass)) {
                return singleFunction;
            }
            return null;
        }
        // 无候选
        if (candidateClasses == null) {
            return null;
        }
        // 小候选数组路径：单趟扫描，精确优先 + 可赋值回退
        if (exactMatches == null) {
            return resolveSmallCandidates(targetClass);
        }
        // 大候选 Map 路径
        return resolveLargeCandidates(targetClass);
    }

    /**
     * 小候选数组路径：单趟扫描
     * 同时处理精确匹配和可赋值回退，避免两次遍历
     */
    @Nullable
    private Function resolveSmallCandidates(@NotNull Class<?> targetClass) {
        Function assignable = null;
        if (candidateClasses != null) {
            for (int i = 0; i < candidateClasses.length; i++) {
                Class<?> c = candidateClasses[i];
                if (c == targetClass && candidateFunctions != null) {
                    return candidateFunctions[i];
                }
                if (assignable == null && c.isAssignableFrom(targetClass) && candidateFunctions != null) {
                    assignable = candidateFunctions[i];
                }
            }
        }
        return assignable;
    }

    /**
     * 大候选 Map 路径：HashMap 精确匹配 + ConcurrentHashMap 缓存
     */
    @Nullable
    private Function resolveLargeCandidates(@NotNull Class<?> targetClass) {
        // 精确匹配: O(1)
        Function exact = null;
        if (exactMatches != null) {
            exact = exactMatches.get(targetClass);
        }
        if (exact != null) {
            return exact;
        }
        // 检查缓存
        Function cached = null;
        if (assignableCache != null) {
            cached = assignableCache.get(targetClass);
        }
        if (cached != null) {
            return cached == NOT_FOUND_SENTINEL ? null : cached;
        }
        // 缓存未命中，执行单趟扫描
        Function assignable = null;
        if (candidateClasses != null) {
            for (int i = 0; i < candidateClasses.length; i++) {
                Class<?> c = candidateClasses[i];
                if (c == targetClass) {
                    // 精确匹配（理论上不应该到这里，因为 exactMatches 已经查过）
                    if (candidateFunctions != null && assignableCache != null) {
                        assignableCache.put(targetClass, candidateFunctions[i]);
                    }
                    if (candidateFunctions != null) {
                        return candidateFunctions[i];
                    }
                }
                if (assignable == null && c.isAssignableFrom(targetClass) && candidateFunctions != null) {
                    assignable = candidateFunctions[i];
                }
            }
        }
        // 缓存结果
        if (assignableCache != null) {
            assignableCache.put(targetClass, assignable != null ? assignable : NOT_FOUND_SENTINEL);
        }
        return assignable;
    }

    /**
     * 获取候选数量（用于测试和调试）
     */
    public int getCandidateCount() {
        if (singleClass != null) return 1;
        if (candidateClasses != null) return candidateClasses.length;
        return 0;
    }

    /**
     * 是否使用 Map 路径（用于测试和调试）
     */
    public boolean usesMapPath() {
        return exactMatches != null;
    }
}
