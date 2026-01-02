package org.tabooproject.fluxon.inst.bytecode;

import org.tabooproject.fluxon.inst.InjectionRegistry;
import org.tabooproject.fluxon.inst.InjectionSpec;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * Fluxon 类文件转换器。
 * 实现 ClassFileTransformer，在类加载/重新转换时注入代码。
 */
public class ClassTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        // 跳过无效类名
        if (className == null) {
            return null;
        }
        // 跳过 Fluxon inst 核心类，避免循环依赖（但允许测试目标类被注入）
        if (className.startsWith("org/tabooproject/fluxon/inst/") && !className.contains("Test$")) {
            return null;
        }
        // 检查是否有针对该类的注入
        InjectionRegistry registry = InjectionRegistry.getInstance();
        if (!registry.hasInjectionsForClass(className)) {
            return null;
        }
        return transformClass(className, classFileBuffer);
    }

    /**
     * 执行实际的类转换。
     */
    private byte[] transformClass(String className, byte[] originalBytecode) {
        BytecodeStore store = BytecodeStore.getInstance();
        InjectionRegistry registry = InjectionRegistry.getInstance();
        // 首次转换时缓存原始字节码
        if (!store.contains(className)) {
            store.store(className, originalBytecode);
        }
        // 获取该类的所有注入规格
        List<InjectionSpec> specs = registry.getSpecsForClass(className);
        if (specs.isEmpty()) {
            // 所有注入都被撤销，恢复原始字节码
            return store.get(className);
        }
        // 从原始字节码开始，应用所有活跃的注入
        byte[] baseBytecode = store.get(className);
        if (baseBytecode == null) {
            baseBytecode = originalBytecode;
        }
        // 使用 MethodInjector 应用注入
        return ClassMethodInjector.inject(baseBytecode, specs);
    }
}