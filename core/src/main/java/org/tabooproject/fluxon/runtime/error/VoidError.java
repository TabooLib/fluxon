package org.tabooproject.fluxon.runtime.error;

import org.tabooproject.fluxon.runtime.FluxonRuntimeError;

/**
 * 使用 Void 参与运算时抛出
 */
public class VoidError extends FluxonRuntimeError {

    public VoidError(String message) {
        super(message);
    }
}
