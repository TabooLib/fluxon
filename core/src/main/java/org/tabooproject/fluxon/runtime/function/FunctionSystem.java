package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

public class FunctionSystem {

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("print", 1, args -> {
            if (args.length > 0) {
                System.out.println(args[0]);
            } else {
                System.out.println();
            }
            return null;
        });
        runtime.registerFunction("error", 1, args -> {
            if (args.length > 0) {
                System.err.println(args[0]);
            } else {
                System.err.println();
            }
            return null;
        });
        runtime.registerFunction("sleep", 1, args -> {
            int seconds = ((Number) args[0]).intValue();
            try {
                Thread.sleep(seconds);
            } catch (InterruptedException e) {
                throw new RuntimeException("Sleep function interrupted", e);
            }
            return null;
        });
    }
}
