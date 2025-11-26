package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.util.ThreadLocalObjectPool;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Environment 池化相关的基本线程安全与重置验证。
 */
public class EnvironmentPoolTest {

    @Test
    public void reuseResetsEnvironmentState() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        EnvironmentPool pool = currentThreadPool();
        resetPoolCounters(pool);

        Map<String, Object> sysVars = runtime.getSystemVariables();

        EnvironmentPool.Lease lease1 = runtime.borrowEnvironment();
        Environment env1 = lease1.get();
        env1.setTarget("T");
        env1.defineRootVariable("custom", 42);
        lease1.close();

        EnvironmentPool.Lease lease2 = runtime.borrowEnvironment();
        Environment env2 = lease2.get();
        assertSame(env1, env2, "Environment instance should be recycled");
        assertNull(env2.getTarget(), "Target should be cleared on reuse");
        assertFalse(env2.getRootVariables().containsKey("custom"), "Custom root variable should not leak across leases");
        assertEquals(sysVars, env2.getRootVariables(), "System variables should be restored on reuse");
        lease2.close();
    }

    @Test
    public void detachSkipsReturnToPool() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        EnvironmentPool pool = currentThreadPool();
        resetPoolCounters(pool);
        int before = getPoolSize(pool);

        EnvironmentPool.Lease lease = runtime.borrowEnvironment();
        Environment env = lease.detach();
        assertNotNull(env);
        lease.close();

        assertEquals(before, getPoolSize(pool), "Detached environment should not be returned to pool");
    }

    @Test
    public void crossThreadCloseIsIgnored() throws Exception {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        EnvironmentPool pool = currentThreadPool();
        resetPoolCounters(pool);

        EnvironmentPool.Lease lease = runtime.borrowEnvironment();
        int afterBorrow = getPoolSize(pool);

        Thread t = new Thread(lease::close);
        t.start();
        t.join();

        assertEquals(afterBorrow, getPoolSize(pool), "Cross-thread close should not affect pool size");
        lease.close(); // close on owner thread to release
    }

    @SuppressWarnings("unchecked")
    private static EnvironmentPool currentThreadPool() throws Exception {
        Field localField = EnvironmentPool.class.getDeclaredField("LOCAL");
        localField.setAccessible(true);
        ThreadLocal<EnvironmentPool> local = (ThreadLocal<EnvironmentPool>) localField.get(null);
        return local.get();
    }

    private static int getPoolSize(ThreadLocalObjectPool<?, ?> pool) throws Exception {
        Field sizeField = ThreadLocalObjectPool.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        return sizeField.getInt(pool);
    }

    private static void resetPoolCounters(ThreadLocalObjectPool<?, ?> pool) throws Exception {
        Field sizeField = ThreadLocalObjectPool.class.getDeclaredField("size");
        Field leaseSizeField = ThreadLocalObjectPool.class.getDeclaredField("leaseSize");
        sizeField.setAccessible(true);
        leaseSizeField.setAccessible(true);
        sizeField.setInt(pool, 0);
        leaseSizeField.setInt(pool, 0);
    }
}
