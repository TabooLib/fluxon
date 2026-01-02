package org.tabooproject.fluxon.inst.agent;

import org.tabooproject.fluxon.inst.InstrumentationBridge;
import org.tabooproject.fluxon.inst.bytecode.ClassTransformer;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

/**
 * Fluxon Java Agent 入口。
 * 支持静态加载（-javaagent）和动态加载（Attach API）。
 */
public class FluxonAgent {

    private static final Logger LOGGER = Logger.getLogger(FluxonAgent.class.getName());
    private static volatile boolean initialized = false;

    /**
     * JVM 启动时通过 -javaagent 参数加载。
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        initialize(inst, "premain");
    }

    /**
     * 通过 Attach API 动态加载。
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        initialize(inst, "agentmain");
    }

    /**
     * 初始化 Agent。
     */
    private static synchronized void initialize(Instrumentation inst, String entryPoint) {
        if (initialized) {
            LOGGER.warning("FluxonAgent 已初始化，忽略重复调用 (" + entryPoint + ")");
            return;
        }
        LOGGER.info("FluxonAgent 正在初始化 (通过 " + entryPoint + ")");
        // 保存 Instrumentation 实例
        InstrumentationHolder.set(inst);
        InstrumentationBridge.initialize(inst);
        // 注册 ClassFileTransformer
        inst.addTransformer(new ClassTransformer(), true);
        initialized = true;
        LOGGER.info("FluxonAgent 初始化完成");
    }

    /**
     * 检查 Agent 是否已初始化。
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
