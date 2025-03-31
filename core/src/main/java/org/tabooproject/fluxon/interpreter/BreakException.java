package org.tabooproject.fluxon.interpreter;

/**
 * 跳出循环
 */
public class BreakException extends RuntimeException {

    public BreakException() {
        super(null, null, false, false); // 禁用堆栈跟踪以提高性能
    }
}