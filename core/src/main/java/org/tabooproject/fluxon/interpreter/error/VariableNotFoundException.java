package org.tabooproject.fluxon.interpreter.error;

/**
 * 没有找到变量异常
 */
public class VariableNotFoundException extends RuntimeException {

    public VariableNotFoundException(String message) {
        super(message);
    }
}
