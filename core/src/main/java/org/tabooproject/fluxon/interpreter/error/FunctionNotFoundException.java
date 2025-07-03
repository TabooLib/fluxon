package org.tabooproject.fluxon.interpreter.error;

/**
 * 没有找到函数异常
 */
public class FunctionNotFoundException extends RuntimeException {

    public FunctionNotFoundException(String message) {
        super(message);
    }
}
