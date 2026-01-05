package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.runtime.Environment;

import java.util.function.Supplier;

/**
 * 域执行器
 * <p>
 * 定义域的执行逻辑。域执行器接收运行时环境和域体闭包，完全控制域体的执行时机和方式。
 * </p>
 *
 * <h3>闭包语义</h3>
 * <p>
 * 域执行器与 {@code ::} 上下文调用的关键区别在于：域执行器接收的是<b>未求值的闭包</b>。
 * 这允许执行器决定：
 * </p>
 * <ul>
 *   <li>是否执行域体（如条件域）</li>
 *   <li>何时执行域体（如异步域）</li>
 *   <li>执行多少次（如重试域）</li>
 *   <li>在什么环境中执行（如事务域）</li>
 *   <li>如何处理返回值（如结果转换）</li>
 * </ul>
 *
 * <h3>统一接口</h3>
 * <p>
 * 无论是解释模式还是编译模式，域执行器只需实现一个方法。
 * 解释器会自动将 AST 包装为 {@code Supplier<Object>}。
 * </p>
 *
 * <h3>上下文传递（通过 target）</h3>
 * <p>
 * 域可以通过 {@link Environment#setTarget(Object)} 设置上下文对象，
 * 使内部代码能够访问域的上下文。这是实现结构化并发、事务等协作模式的基础。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 异步执行域
 * DomainExecutor asyncDomain = (env, body) -> {
 *     return CompletableFuture.supplyAsync(body::get);
 * };
 *
 * // 重试域：失败时重试 3 次
 * DomainExecutor retryDomain = (env, body) -> {
 *     int maxAttempts = 3;
 *     for (int i = 0; i < maxAttempts; i++) {
 *         try {
 *             return body.get();
 *         } catch (Exception e) {
 *             if (i == maxAttempts - 1) throw e;
 *         }
 *     }
 *     return null;
 * };
 *
 * // 结构化并发：通过 target 传递作用域
 * DomainExecutor scopeDomain = (env, body) -> {
 *     TaskScope scope = new TaskScope();
 *     Object previousTarget = env.getTarget();
 *     try {
 *         env.setTarget(scope);
 *         Object result = body.get();
 *         scope.join();
 *         return result;
 *     } finally {
 *         env.setTarget(previousTarget);
 *     }
 * };
 *
 * // time 域：设置 target 类似 ::
 * DomainExecutor timeDomain = (env, body) -> {
 *     long start = System.currentTimeMillis();
 *     Object previousTarget = env.getTarget();
 *     try {
 *         env.setTarget(start);
 *         return body.get();
 *     } finally {
 *         env.setTarget(previousTarget);
 *         System.out.println("Elapsed: " + (System.currentTimeMillis() - start) + "ms");
 *     }
 * };
 * }</pre>
 *
 * @see DomainRegistry
 * @see Environment#setTarget(Object)
 * @see Environment#getTarget()
 */
@FunctionalInterface
public interface DomainExecutor {

    /**
     * 执行域逻辑
     * <p>
     * 接收运行时环境和域体闭包，执行器完全控制域体的执行。
     * </p>
     *
     * @param environment 运行时环境，可用于访问变量和设置 target
     * @param body        域体闭包（调用 get() 执行域体）
     * @return 域的返回值
     * @throws Exception 如果执行过程中发生错误
     */
    Object execute(@NotNull Environment environment, @NotNull Supplier<Object> body) throws Exception;
}
