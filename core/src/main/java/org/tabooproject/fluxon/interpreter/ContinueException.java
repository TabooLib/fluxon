package org.tabooproject.fluxon.interpreter;

/**
 * 跳出循环
 */
public class ContinueException extends RuntimeException {

    public ContinueException() {
        super(null, null, false, false); // 禁用堆栈跟踪以提高性能
    }
}