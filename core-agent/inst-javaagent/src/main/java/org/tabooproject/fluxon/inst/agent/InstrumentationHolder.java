package org.tabooproject.fluxon.inst.agent;

import java.lang.instrument.Instrumentation;

/**
 * Instrumentation 持有者。
 */
public final class InstrumentationHolder {

    public static final InstrumentationHolder INSTANCE = new InstrumentationHolder();
    
    private static volatile Instrumentation instrumentation;

    private InstrumentationHolder() {
    }

    static void set(Instrumentation inst) {
        instrumentation = inst;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void retransform(Class<?>... classes) throws Exception {
        if (instrumentation == null) {
            throw new IllegalStateException("Instrumentation 不可用");
        }
        instrumentation.retransformClasses(classes);
    }
}
