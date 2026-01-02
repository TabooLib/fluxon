package org.tabooproject.fluxon.inst.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.logging.Logger;

/**
 * Agent 端类转换器。
 * 直接访问 AgentRegistry，避免类加载器隔离问题。
 */
public class AgentTransformer implements ClassFileTransformer {

    private static final Logger LOGGER = Logger.getLogger(AgentTransformer.class.getName());

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        if (className == null) {
            return null;
        }
        // 跳过 Agent 自身的类
        if (className.startsWith("org/tabooproject/fluxon/inst/agent/")) {
            return null;
        }
        // 检查是否有针对该类的注入
        if (!AgentRegistry.INSTANCE.hasInjectionsForClass(className)) {
            return null;
        }
        // LOGGER.info("正在转换类: " + className + (classBeingRedefined != null ? " (retransform)" : ""));
        try {
            // byte[] result = doTransform(className, classFileBuffer);
            // LOGGER.info("类转换完成: " + className);
            return doTransform(className, classFileBuffer);
        } catch (Exception e) {
            LOGGER.severe("类转换失败: " + className + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private byte[] doTransform(String className, byte[] bytecode) {
        List<String[]> specs = AgentRegistry.INSTANCE.getSpecsForClass(className);
        if (specs.isEmpty()) {
            return null;
        }
        // 调用 inst-core 的字节码注入器（通过反射，因为可能在不同类加载器）
        return injectBytecode(bytecode, specs);
    }

    /**
     * 执行字节码注入。
     * 由于 ASM 在 Agent jar 中，直接使用。
     */
    private byte[] injectBytecode(byte[] bytecode, List<String[]> specs) {
        return AgentBytecodeInjector.inject(bytecode, specs);
    }
}
