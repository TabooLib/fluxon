package org.tabooproject.fluxon.runtime;

/**
 * Fluxon 运行时错误基类
 * 所有运行时错误都应该继承此类
 */
public abstract class FluxonRuntimeError extends RuntimeException {

    protected FluxonRuntimeError(String message) {
        super(message);
    }

    protected FluxonRuntimeError(String message, Throwable cause) {
        super(message, cause);
    }
}
