package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.util.KV;
import org.tabooproject.fluxon.util.ThreadLocalObjectPool;

import java.util.Map;

/**
 * 线程本地的 Environment 池，避免频繁创建根环境对象。
 */
public final class EnvironmentPool extends ThreadLocalObjectPool<Environment, EnvironmentPool.Lease> {

    private static final int MAX_POOL_SIZE = 32;
    private static final ThreadLocal<EnvironmentPool> LOCAL = ThreadLocal.withInitial(EnvironmentPool::new);

    private Map<String, Function> functions;
    private Function[] systemFunctions;
    private Map<String, Object> values;
    private Map<String, Map<Class<?>, Function>> extensionFunctions;
    private KV<Class<?>, Function>[][] systemExtensionFunctions;

    private EnvironmentPool() {
        super(MAX_POOL_SIZE);
    }

    /**
     * 借用一个根环境实例。
     */
    public static Lease borrow(@NotNull Map<String, Function> functions,
                               @NotNull Function[] systemFunctions,
                               @NotNull Map<String, Object> values,
                               @NotNull Map<String, Map<Class<?>, Function>> extensionFunctions,
                               @NotNull KV<Class<?>, Function>[][] systemExtensionFunctions) {
        EnvironmentPool pool = LOCAL.get();
        pool.functions = functions;
        pool.systemFunctions = systemFunctions;
        pool.values = values;
        pool.extensionFunctions = extensionFunctions;
        pool.systemExtensionFunctions = systemExtensionFunctions;
        return pool.borrowInternal();
    }

    @Override
    protected Environment create() {
        return new Environment(functions, systemFunctions, values, extensionFunctions, systemExtensionFunctions);
    }

    @Override
    protected void onBorrow(Environment value) {
        value.resetRoot(functions, systemFunctions, values, extensionFunctions, systemExtensionFunctions);
    }

    @Override
    protected void onRelease(Environment value) {
        value.clearForPooling();
    }

    @Override
    protected Lease newLease() {
        return new Lease();
    }

    public static final class Lease extends ThreadLocalObjectPool.Lease<Environment> {
    }
}
