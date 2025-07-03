package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

public class FunctionMath {

    public static void init() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("min", 2, args -> {
            if (Operations.compare(args[0], args[1]) > 0) {
                return args[0];
            }
            return args[1];
        });
        runtime.registerFunction("max", 2, args -> {
            if (Operations.compare(args[0], args[1]) < 0) {
                return args[0];
            }
            return args[1];
        });
        runtime.registerFunction("abs", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("round", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("floor", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("ceil", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("pow", 2, args -> {
            Operations.checkNumberOperand(args[0]);
            Operations.checkNumberOperand(args[1]);
            return null;
        });
        runtime.registerFunction("sqrt", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("sin", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("cos", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("tan", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("asin", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("acos", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("atan", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("exp", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("log", 1, args -> {
            Operations.checkNumberOperand(args[0]);
            return null;
        });
        runtime.registerFunction("random", 0, args -> {
            return null;
        });
    }
}
