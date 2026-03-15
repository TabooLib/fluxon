package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

public class ExtensionThrowable {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionThrowable.class);
    }

    @FluxonFunction(value = "message", target = Throwable.class)
    public static Object message(Throwable t) {
        return t.getMessage();
    }

    @FluxonFunction(value = "localizedMessage", target = Throwable.class)
    public static Object localizedMessage(Throwable t) {
        return t.getLocalizedMessage();
    }

    @FluxonFunction(value = "cause", target = Throwable.class)
    public static Object cause(Throwable t) {
        return t.getCause();
    }

    @FluxonFunction(value = "printStackTrace", target = Throwable.class)
    public static void printStackTraceExt(Throwable t) {
        t.printStackTrace();
    }
}
