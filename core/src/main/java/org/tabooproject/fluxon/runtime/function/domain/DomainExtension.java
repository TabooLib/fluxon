package org.tabooproject.fluxon.runtime.function.domain;

import org.tabooproject.fluxon.parser.DomainRegistry;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.concurrent.TaskScope;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class DomainExtension {

    public static void init() {
        // with - 执行结束返回闭包最后一行
        DomainRegistry.primary().register("with", (env, body) -> body.get());
        // also - 执行结束返回 target
        DomainRegistry.primary().register("also", (env, body) -> {
            body.get();
            return env.getTarget();
        });
        // scope - 结构化并发作用域
        DomainRegistry.primary().register("scope", DomainExtension::executeScope);
        // runAsync - 异步执行任务
        DomainRegistry.primary().register("runAsync", DomainExtension::executeAsync);
        // runSync - 在主线程执行器中执行任务
        DomainRegistry.primary().register("runSync", DomainExtension::executeSync);
    }

    /**
     * Domain 执行器入口
     * <p>
     * 用法：
     * <pre>{@code
     * result = scope {
     *     runAsync { task1() }
     *     runSync { task2() }
     * }
     * await &result  // 等待所有任务完成
     * }</pre>
     *
     * @return CompletableFuture，包装 scope 的执行结果
     */
    public static Object executeScope(Environment environment, Supplier<Object> body) {
        return CompletableFuture.supplyAsync(() -> {
            TaskScope scope = new TaskScope(environment);
            Object previousTarget = environment.getTarget();
            try {
                // 设置 target 为 scope，使内部的 async 可以访问
                environment.setTarget(scope);
                // 执行 scope 体
                Object result = body.get();
                // 自动等待所有任务完成
                scope.join();
                return result;
            } catch (Exception e) {
                throw new RuntimeException("Scope execution failed", e);
            } finally {
                environment.setTarget(previousTarget);
                scope.close();
            }
        });
    }

    /**
     * async domain 执行器
     */
    private static Object executeAsync(Environment environment, Supplier<Object> body) {
        Object target = environment.getTarget();
        // 如果在 TaskScope 内，将任务注册到 scope
        if (target instanceof TaskScope) {
            TaskScope scope = (TaskScope) target;
            return scope.submit(body);
        }
        // 否则独立执行
        return CompletableFuture.supplyAsync(body);
    }

    /**
     * sync domain 执行器
     */
    private static Object executeSync(Environment environment, Supplier<Object> body) {
        Object target = environment.getTarget();
        // 获取主线程执行器
        Executor primaryExecutor = FluxonRuntime.getInstance().getPrimaryThreadExecutor();
        // 如果在 TaskScope 内，将任务注册到 scope（但使用主线程执行器）
        if (target instanceof TaskScope) {
            TaskScope scope = (TaskScope) target;
            return scope.submitSync(body, primaryExecutor);
        }
        // 否则独立执行
        return CompletableFuture.supplyAsync(body, primaryExecutor);
    }
}