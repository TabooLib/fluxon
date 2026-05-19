package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 线程本地的 FunctionContext 栈式分配器
 * 预分配固定数量的 context，通过 depth 计数器实现 O(1) 借/还。
 * 同步调用天然满足 LIFO 顺序，无需 dirty 跟踪和 clearRefs。
 * 异步调用通过 detach 转移到堆上，不影响栈状态。
 */
public final class FunctionContextPool {

    public static final Type TYPE = new Type(FunctionContextPool.class);

    private static final int MAX_DEPTH = 64;
    private static final int INITIAL_CAPACITY = 8;
    private static final ThreadLocal<FunctionContextPool> LOCAL = ThreadLocal.withInitial(FunctionContextPool::new);
    private static final Object[] EMPTY_REFS = new Object[0];

    private final FunctionContext<?>[] stack;
    private int depth;
    private int usedDepth;

    private FunctionContextPool() {
        stack = new FunctionContext<?>[MAX_DEPTH];
        for (int i = 0; i < MAX_DEPTH; i++) {
            stack[i] = new FunctionContext<>(this, INITIAL_CAPACITY, i);
        }
    }

    /**
     * 获取当前线程的池实例
     */
    @NotNull
    public static FunctionContextPool local() {
        return LOCAL.get();
    }

    /**
     * 从栈中借用一个 FunctionContext 实例
     */
    public FunctionContext<?> borrow(@NotNull Function function, @Nullable Object target, @NotNull Object[] refs, @NotNull Environment environment) {
        FunctionContext<?> context = acquire();
        context.reset(function, target, refs, environment);
        return context;
    }

    /**
     * 从栈中借用一个 FunctionContext 实例（按参数数量，复用内部数组）
     */
    public FunctionContext<?> borrow(@NotNull Function function, @Nullable Object target, int argCount, @NotNull Environment environment) {
        FunctionContext<?> context = acquire();
        context.reset(function, target, argCount, environment);
        return context;
    }

    /**
     * 从栈中借用一个 FunctionContext 实例（复制已有 context 的函数/目标/环境）
     */
    public FunctionContext<?> borrowCopy(@NotNull FunctionContext<?> context, @Nullable Object[] refs) {
        return borrow(context.getFunction(), context.getTarget(), refs != null ? refs : EMPTY_REFS, context.getEnvironment());
    }

    /**
     * 同步热路径专用归还，跳过 stackIndex 校验
     * 调用者保证 LIFO 顺序（同步解释执行天然满足）
     */
    public void releaseTop() {
        depth--;
    }

    /**
     * 归还 context，由 FunctionContext.close() 调用
     * 栈式分配保证 LIFO 顺序，只需递减 depth
     */
    void releaseUnchecked(FunctionContext<?> context) {
        if (context.stackIndex == depth - 1) {
            depth--;
        }
    }

    int getDepth() {
        return depth;
    }

    /**
     * 清理当前线程空闲池槽中残留的上次脚本对象图
     */
    public void clearIdleContexts() {
        if (depth != 0) {
            return;
        }
        for (int i = 0; i < usedDepth; i++) {
            stack[i].clearIdleReferences();
        }
        usedDepth = 0;
    }

    /**
     * 分离 context（async 转移所有权）
     * 用新 context 替换栈槽位，使 close() 时身份校验自然失败
     * 同时回收 depth 以防泄漏：栈顶 context 被分离后，该槽位立即可复用
     */
    void detach(FunctionContext<?> context) {
        int idx = context.stackIndex;
        if (idx >= 0 && idx < MAX_DEPTH && stack[idx] == context) {
            stack[idx] = new FunctionContext<>(this, INITIAL_CAPACITY, idx);
            // 栈顶分离时回收 depth，避免 async 调用累积导致 depth 泄漏
            if (idx == depth - 1) {
                depth--;
            }
        } else if (idx == -1 && depth > MAX_DEPTH) {
            // 堆溢出 context 的分离也回收 depth
            depth--;
        }
        context.stackIndex = -1;
    }

    private FunctionContext<?> acquire() {
        if (depth < MAX_DEPTH) {
            FunctionContext<?> context = stack[depth++];
            if (depth > usedDepth) {
                usedDepth = depth;
            }
            return context;
        }
        // 溢出时分配堆上的 context，stackIndex = -1 表示不在栈中
        // 仍然递增 depth 以保持 borrow/releaseTop 配对平衡
        depth++;
        return new FunctionContext<>(this);
    }
}
