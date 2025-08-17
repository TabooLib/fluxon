package org.tabooproject.fluxon.interpreter.error;

import org.tabooproject.fluxon.runtime.Function;

import java.util.Map;

/**
 * 没有找到函数异常
 */
public class FunctionNotFoundException extends RuntimeException {

    public FunctionNotFoundException(String message) {
        super(message);
    }

    public FunctionNotFoundException(String message, Map<String, Function> functions) {
        super(message + ", functions: " + functions.keySet());
    }
}
