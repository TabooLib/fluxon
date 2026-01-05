package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 域注册表
 * <p>
 * 管理自定义域的注册和查找。域（Domain）提供了比 {@code ::} 上下文调用更强大的控制流机制，
 * 允许开发者完全控制代码块的执行时机、方式和环境。
 * </p>
 *
 * <h3>域 vs. 上下文调用</h3>
 * <table border="1">
 *   <tr>
 *     <th>特性</th>
 *     <th>上下文调用 {@code ::}</th>
 *     <th>域语法</th>
 *   </tr>
 *   <tr>
 *     <td>语法</td>
 *     <td>{@code value :: block}</td>
 *     <td>{@code domain block}</td>
 *   </tr>
 *   <tr>
 *     <td>块执行</td>
 *     <td>立即执行，无法延迟</td>
 *     <td>执行器控制执行时机</td>
 *   </tr>
 *   <tr>
 *     <td>闭包语义</td>
 *     <td>不支持（块立即求值）</td>
 *     <td>支持（接收未求值的 AST）</td>
 *   </tr>
 *   <tr>
 *     <td>适用场景</td>
 *     <td>简单上下文传递</td>
 *     <td>异步、事务、重试、结构化并发</td>
 *   </tr>
 * </table>
 *
 * <h3>使用模式</h3>
 * <p>
 * DomainRegistry 支持两种使用模式：
 * </p>
 * <ul>
 *   <li><b>静态主注册表</b> - 通过 {@link #primary()} 访问的全局注册表，适用于大多数场景</li>
 *   <li><b>独立实例</b> - 通过构造函数创建的独立注册表，用于沙箱隔离或多租户场景</li>
 * </ul>
 *
 * <h3>使用示例 - 基本域注册</h3>
 * <pre>{@code
 * // 注册异步域到主注册表
 * DomainRegistry.primary().register("async", (env, bodyAst) -> {
 *     return CompletableFuture.supplyAsync(() -> {
 *         return DomainSupport.evaluateBody(bodyAst, env);
 *     });
 * });
 *
 * // 在脚本中使用
 * // async { sleep(100); result = fetch("http://api.example.com") }
 *
 * // 注册重试域
 * DomainRegistry.primary().register("retry", (env, bodyAst) -> {
 *     int maxAttempts = 3;
 *     for (int i = 0; i < maxAttempts; i++) {
 *         try {
 *             return DomainSupport.evaluateBody(bodyAst, env);
 *         } catch (Exception e) {
 *             if (i == maxAttempts - 1) throw e;
 *         }
 *     }
 *     return null;
 * });
 *
 * // 在脚本中使用
 * // retry { unstableOperation() }
 * }</pre>
 *
 * <h3>使用示例 - 结构化并发（通过 target）</h3>
 * <pre>{@code
 * // 注册 scope 域
 * DomainRegistry.primary().register("scope", (env, bodyAst) -> {
 *     TaskScope scope = new TaskScope();
 *     Object previousTarget = env.getTarget();
 *     try {
 *         // 设置 target，使内部代码能访问这个 scope
 *         env.setTarget(scope);
 *         Object result = DomainSupport.evaluateBody(bodyAst, env);
 *         scope.join(); // 等待所有子任务完成
 *         return result;
 *     } finally {
 *         env.setTarget(previousTarget);
 *     }
 * });
 *
 * // 注册 async 域，支持从 target 获取父 scope
 * DomainRegistry.primary().register("async", (env, bodyAst) -> {
 *     Object target = env.getTarget();
 *     
 *     // 如果在 scope 内，将任务注册到 scope
 *     if (target instanceof TaskScope) {
 *         TaskScope scope = (TaskScope) target;
 *         return scope.submit(() -> DomainSupport.evaluateBody(bodyAst, env));
 *     }
 *     
 *     // 否则独立执行
 *     return CompletableFuture.supplyAsync(() -> {
 *         return DomainSupport.evaluateBody(bodyAst, env);
 *     });
 * });
 *
 * // 在脚本中使用 - 结构化并发
 * // scope {
 * //     task1 = async { longOperation1() }  // 注册到 scope
 * //     task2 = async { longOperation2() }  // 注册到 scope
 * //     // scope.join() 等待所有任务完成
 * // }
 * }</pre>
 *
 * <h3>Target 类型检查约定</h3>
 * <pre>{@code
 * // 推荐使用 instanceof 进行类型检查
 * Object target = env.getTarget();
 * if (target instanceof TaskScope) {
 *     TaskScope scope = (TaskScope) target;
 *     // 使用 scope
 * } else if (target instanceof TransactionContext) {
 *     TransactionContext tx = (TransactionContext) target;
 *     // 使用事务上下文
 * }
 * }</pre>
 *
 * <h3>线程安全</h3>
 * <p>
 * DomainRegistry 实例在注册阶段（通常在应用启动时）应在单线程环境下操作。
 * 注册完成后，多个线程可以并发调用 {@link #hasDomain} 和 {@link #get} 方法进行只读访问，无需额外同步。
 * </p>
 *
 * @see DomainExecutor
 * @see org.tabooproject.fluxon.runtime.Environment#setTarget(Object)
 * @see org.tabooproject.fluxon.runtime.Environment#getTarget()
 */
public class DomainRegistry {

    /**
     * 静态主注册表实例
     */
    private static final DomainRegistry PRIMARY = new DomainRegistry();

    /**
     * 域映射表：domainName -> DomainExecutor
     */
    private final Map<String, DomainExecutor> domains;

    /**
     * 获取静态主注册表
     * <p>
     * 这是全局默认的域注册表，适用于大多数场景。
     * 如果 {@link org.tabooproject.fluxon.compiler.CompilationContext} 未显式设置 DomainRegistry，
     * 将自动使用此主注册表。
     * </p>
     *
     * @return 主注册表实例
     */
    @NotNull
    public static DomainRegistry primary() {
        return PRIMARY;
    }

    /**
     * 创建一个包含主注册表所有域的新注册表实例
     * <p>
     * 用于在主注册表的基础上创建隔离的注册表副本。
     * </p>
     *
     * @return 包含主注册表所有域的新实例
     */
    @NotNull
    public static DomainRegistry withDefaults() {
        DomainRegistry registry = new DomainRegistry();
        registry.domains.putAll(primary().domains);
        return registry;
    }

    /**
     * 创建新的独立 DomainRegistry 实例
     */
    public DomainRegistry() {
        this.domains = new HashMap<>();
    }

    /**
     * 注册域
     * <p>
     * 将指定名称的域及其执行器注册到此注册表。
     * 如果域名称已存在，将覆盖之前的注册。
     * </p>
     *
     * @param domainName 域名称（标识符），如 "async", "transaction"
     * @param executor   域执行器，负责控制域体的执行
     * @throws IllegalArgumentException 如果任何参数为 null 或空
     */
    public void register(@NotNull String domainName, @NotNull DomainExecutor executor) {
        if (domainName.isEmpty()) {
            throw new IllegalArgumentException("Domain name cannot be null or empty");
        }
        domains.put(domainName, executor);
    }

    /**
     * 检查域是否已注册
     *
     * @param domainName 域名称
     * @return 如果域已注册返回 true，否则返回 false
     */
    public boolean hasDomain(@NotNull String domainName) {
        return domains.containsKey(domainName);
    }

    /**
     * 获取已注册的域执行器
     *
     * @param domainName 域名称
     * @return 对应的 DomainExecutor，如果未注册返回 null
     */
    @Nullable
    public DomainExecutor get(@NotNull String domainName) {
        return domains.get(domainName);
    }

    /**
     * 获取所有已注册的域名称
     * <p>
     * 返回的 Map 是只读的，修改不会影响注册表。
     * </p>
     *
     * @return 不可变的域映射表
     */
    @NotNull
    public Map<String, DomainExecutor> getDomains() {
        return new HashMap<>(domains);
    }

    /**
     * 清空所有已注册的域
     * <p>
     * 警告：此操作不可逆，主要用于测试环境。生产环境应避免使用。
     * </p>
     */
    public void clear() {
        domains.clear();
    }
}
