package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.CommandRegistry;
import org.tabooproject.fluxon.parser.DomainRegistry;
import org.tabooproject.fluxon.runtime.collection.CopyOnWriteMap;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 环境根状态
 * <p>
 * 持有跨作用域共享的全局数据，仅在根环境上分配。
 * 子环境通过 {@code root.rootState} 引用同一实例，避免重复分配。
 *
 * @author sky
 * @see Environment
 */
public final class EnvironmentState {

    // 系统函数 + 用户定义函数（CopyOnWrite 隔离）
    @NotNull
    final Map<String, OverloadSet> functions;
    // 用户动态定义的函数名称（区分系统函数，用于增量解析）
    @Nullable
    Set<String> userFunctionNames;

    // 根变量表（CopyOnWrite 隔离）
    @NotNull
    final Map<String, Object> rootVariables;

    // IO
    @NotNull
    PrintStream out;
    @NotNull
    PrintStream err;

    @Nullable
    CommandRegistry commandRegistry;
    @Nullable
    DomainRegistry domainRegistry;

    // 执行成本控制
    boolean costLimitEnabled;
    long costLimit = Long.MAX_VALUE;
    final AtomicLong costRemaining = new AtomicLong(Long.MAX_VALUE);
    long costPerStep = 1L;

    EnvironmentState(@NotNull Map<String, OverloadSet> functions, @NotNull Map<String, Object> rootVariables) {
        this.functions = CopyOnWriteMap.wrap(functions);
        this.rootVariables = CopyOnWriteMap.wrap(rootVariables);
        this.out = System.out;
        this.err = System.err;
    }
}
