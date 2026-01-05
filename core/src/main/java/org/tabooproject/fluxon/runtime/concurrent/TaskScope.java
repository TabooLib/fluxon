package org.tabooproject.fluxon.runtime.concurrent;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 结构化并发作用域
 * <p>
 * 类似 Kotlin 协程的 CoroutineScope，用于管理并发任务的生命周期。
 * 所有在 scope 内启动的 async 任务会自动注册到 scope，并在 scope 结束时等待完成。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 结构化并发
 * result = scope {
 *     runAsync { longOperation1() }
 *     runAsync { longOperation2() }
 *     // scope 会自动等待所有任务完成
 * }
 *
 * // 手动等待
 * future = scope {
 *     runAsync { work1() }
 *     runAsync { work2() }
 * }
 * await &await
 * }</pre>
 */
public class TaskScope implements AutoCloseable {

    private final Executor executor;
    private final List<CompletableFuture<?>> tasks;
    private final Environment environment;
    private volatile boolean cancelled;

    public TaskScope(Environment environment) {
        this(environment, null);
    }

    public TaskScope(Environment environment, Executor executor) {
        this.environment = environment;
        this.executor = executor != null ? executor : ThreadPoolManager.getInstance().getExecutorService();
        this.tasks = new CopyOnWriteArrayList<>();
        this.cancelled = false;
    }

    /**
     * 在此 scope 中提交一个异步任务
     *
     * @param task 任务闭包
     * @return CompletableFuture，可以被 await 等待
     */
    public CompletableFuture<Object> submit(Supplier<Object> task) {
        if (cancelled) {
            throw new IllegalStateException("TaskScope is already cancelled");
        }
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(task, executor);
        tasks.add(future);
        return future;
    }

    /**
     * 在指定执行器中提交一个任务（用于 sync domain）
     *
     * @param task 任务闭包
     * @param executor 执行器
     * @return CompletableFuture，可以被 await 等待
     */
    public CompletableFuture<Object> submitSync(Supplier<Object> task, Executor executor) {
        if (cancelled) {
            throw new IllegalStateException("TaskScope is already cancelled");
        }
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(task, executor);
        tasks.add(future);
        return future;
    }

    /**
     * 等待所有任务完成
     *
     * @return scope 最后一个表达式的值（null）
     * @throws Exception 如果任何任务失败
     */
    public Object join() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        // 等待所有任务完成
        for (CompletableFuture<?> future : tasks) {
            try {
                future.get();
            } catch (ExecutionException e) {
                errors.add(e.getCause() != null ? e.getCause() : e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
        // 如果有错误，抛出第一个
        if (!errors.isEmpty()) {
            Throwable first = errors.get(0);
            if (first instanceof Exception) {
                throw (Exception) first;
            } else {
                throw new RuntimeException("Task execution failed", first);
            }
        }
        return null;
    }

    /**
     * 取消所有任务
     */
    public void cancel() {
        cancelled = true;
        for (CompletableFuture<?> future : tasks) {
            future.cancel(true);
        }
    }

    @Override
    public void close() {
        // 不关闭 executor，因为可能是共享的（如 ThreadPoolManager）
    }

    public Executor getExecutor() {
        return executor;
    }

    public List<CompletableFuture<?>> getTasks() {
        return new ArrayList<>(tasks);
    }

    public Environment getEnvironment() {
        return environment;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
