package org.tabooproject.fluxon.runtime.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池管理器
 * 管理 Fluxon 运行时的线程池资源
 * 
 * @author sky
 */
public class ThreadPoolManager {
    
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 1000;
    
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    
    /**
     * 单例实例
     */
    private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();
    
    private ThreadPoolManager() {
        // 创建核心线程池
        this.executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new FluxonThreadFactory("fluxon-worker"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 创建调度线程池
        this.scheduledExecutorService = Executors.newScheduledThreadPool(
                Math.max(2, CORE_POOL_SIZE / 4),
                new FluxonThreadFactory("fluxon-scheduler")
        );
    }
    
    /**
     * 获取线程池管理器实例
     */
    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 提交异步任务
     * 
     * @param task 要执行的任务
     * @return CompletableFuture 对象
     */
    public <T> CompletableFuture<T> submitAsync(Callable<T> task) {
        activeTaskCount.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                activeTaskCount.decrementAndGet();
            }
        }, executorService);
    }
    
    /**
     * 提交异步任务（无返回值）
     * 
     * @param task 要执行的任务
     * @return CompletableFuture 对象
     */
    public CompletableFuture<Void> runAsync(Runnable task) {
        activeTaskCount.incrementAndGet();
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } finally {
                activeTaskCount.decrementAndGet();
            }
        }, executorService);
    }
    
    /**
     * 延迟执行任务
     * 
     * @param task 要执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return ScheduledFuture 对象
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduledExecutorService.schedule(task, delay, unit);
    }
    
    /**
     * 周期性执行任务
     * 
     * @param task 要执行的任务
     * @param initialDelay 初始延迟
     * @param period 执行周期
     * @param unit 时间单位
     * @return ScheduledFuture 对象
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutorService.scheduleAtFixedRate(task, initialDelay, period, unit);
    }
    
    /**
     * 获取当前活跃任务数
     */
    public int getActiveTaskCount() {
        return activeTaskCount.get();
    }

    /**
     * 获取核心线程池
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * 获取调度线程池
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
        scheduledExecutorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 自定义线程工厂
     */
    private static class FluxonThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        FluxonThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}