package org.tabooproject.fluxon.inst.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

/**
 * Fluxon Java Agent 入口。
 */
public class FluxonAgent {

    private static final Logger LOGGER = Logger.getLogger(FluxonAgent.class.getName());
    private static volatile boolean initialized = false;

    public static void premain(String agentArgs, Instrumentation inst) {
        initialize(inst, "premain");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        initialize(inst, "agentmain");
    }

    private static synchronized void initialize(Instrumentation inst, String entryPoint) {
        if (initialized) {
            LOGGER.warning("FluxonAgent 已初始化，忽略重复调用 (" + entryPoint + ")");
            return;
        }
        LOGGER.info("FluxonAgent 正在初始化 (通过 " + entryPoint + ")");
        InstrumentationHolder.set(inst);
        inst.addTransformer(new AgentTransformer(), true);
        initialized = true;
        LOGGER.info("FluxonAgent 初始化完成");
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
