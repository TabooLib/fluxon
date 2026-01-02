package org.tabooproject.fluxon.inst.agent;

import java.lang.instrument.Instrumentation;

/**
 * Instrumentation 持有者。
 * 全局静态持有 JVM 提供的 Instrumentation 实例。
 */
public final class InstrumentationHolder {

    private static volatile Instrumentation instrumentation;

    private InstrumentationHolder() {
    }

    /**
     * 设置 Instrumentation 实例。
     * 由 FluxonAgent 在 premain/agentmain 时调用。
     */
    static void set(Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * 获取 Instrumentation 实例。
     * @return Instrumentation 实例，如果 Agent 未加载则返回 null
     */
    public static Instrumentation get() {
        return instrumentation;
    }

    /**
     * 检查 Instrumentation 是否可用。
     */
    public static boolean isAvailable() {
        return instrumentation != null;
    }
}
