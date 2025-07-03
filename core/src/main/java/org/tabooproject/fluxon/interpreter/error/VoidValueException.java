package org.tabooproject.fluxon.interpreter.error;

/**
 * 使用 Void 参与运算时抛出
 */
public class VoidValueException extends RuntimeException {

    public VoidValueException(String message) {
        super(message);
    }
}
