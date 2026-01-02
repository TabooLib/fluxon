package org.tabooproject.fluxon.inst;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Instrumentation 桥接类。
 * 通过反射从 Agent 类加载器获取 Instrumentation。
 */
public final class InstrumentationBridge {

    private static final String HOLDER_CLASS = "org.tabooproject.fluxon.inst.agent.InstrumentationHolder";
    
    private static volatile Object holder;            // InstrumentationHolder.INSTANCE
    private static volatile Method getInstMethod;     // getInstrumentation()
    private static volatile Method retransformMethod; // retransform(Class<?>...)
    private static volatile boolean initialized = false;

    private InstrumentationBridge() {
    }

    private static synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> holderClass = ClassLoader.getSystemClassLoader().loadClass(HOLDER_CLASS);
            Field instanceField = holderClass.getField("INSTANCE");
            holder = instanceField.get(null);
            getInstMethod = holderClass.getMethod("getInstrumentation");
            retransformMethod = holderClass.getMethod("retransform", Class[].class);
        } catch (Exception e) {
            System.err.println("[InstrumentationBridge] 初始化失败: " + e.getMessage());
        }
    }

    public static Instrumentation get() {
        ensureInitialized();
        if (holder == null || getInstMethod == null) return null;
        try {
            return (Instrumentation) getInstMethod.invoke(holder);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAvailable() {
        return get() != null;
    }

    public static void retransformClasses(Class<?>... classes) {
        ensureInitialized();
        if (holder == null || retransformMethod == null) {
            throw new IllegalStateException("Instrumentation 不可用，Agent 是否已加载？");
        }
        try {
            retransformMethod.invoke(holder, (Object) classes);
        } catch (Exception e) {
            throw new RuntimeException("重新转换类失败: " + e.getMessage(), e);
        }
    }
}
