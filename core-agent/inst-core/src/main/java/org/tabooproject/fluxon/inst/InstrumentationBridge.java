package org.tabooproject.fluxon.inst;

import java.lang.instrument.Instrumentation;

/**
 * Instrumentation 桥接类。
 * 解耦 inst-core 与 inst-javaagent 的 InstrumentationHolder。
 */
public final class InstrumentationBridge {

    private static volatile Instrumentation instrumentation;

    private InstrumentationBridge() {
    }

    /**
     * 初始化桥接，设置 Instrumentation 实例。
     * 由 FluxonAgent 在初始化时调用。
     */
    public static void initialize(Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * 获取 Instrumentation 实例。
     * @return Instrumentation 实例，未初始化则返回 null
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

    /**
     * 使用已注册的 transformer 重新转换指定的类。
     * @param classes 要重新转换的类
     * @throws IllegalStateException 如果 Instrumentation 不可用
     */
    public static void retransformClasses(Class<?>... classes) {
        Instrumentation inst = instrumentation;
        if (inst == null) {
            throw new IllegalStateException("Instrumentation 不可用，Agent 是否已加载？");
        }
        try {
            inst.retransformClasses(classes);
        } catch (Exception e) {
            throw new RuntimeException("重新转换类失败", e);
        }
    }
}
