package org.tabooproject.fluxon.runtime.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 针对 Fluxon 异步执行器运行时选择的回归测试
 */
public class ThreadPoolManagerTest {

    /**
     * 验证支持虚拟线程的运行时会自动启用虚拟线程执行器。
     */
    @Test
    public void asyncExecutorUsesVirtualThreadsWhenRuntimeSupportsIt() throws Exception {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        boolean expectedVirtualThreadExecutor = supportsVirtualThreadExecutor();
        assertEquals(expectedVirtualThreadExecutor, manager.isVirtualThreadExecutor(),
                "ThreadPoolManager should match runtime virtual thread support");

        String thread = manager.submitAsync(() -> Thread.currentThread().toString()).get(5, TimeUnit.SECONDS);
        if (expectedVirtualThreadExecutor) {
            assertTrue(thread.contains("VirtualThread"),
                    "Async task should run on a virtual thread when the runtime supports it");
        } else {
            assertTrue(thread.contains("fluxon-worker-"),
                    "Async task should fall back to the bounded worker pool on older runtimes");
        }
    }

    /**
     * 通过反射探测虚拟线程 API，保持 Java 8 编译目标兼容。
     */
    private boolean supportsVirtualThreadExecutor() {
        try {
            Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
