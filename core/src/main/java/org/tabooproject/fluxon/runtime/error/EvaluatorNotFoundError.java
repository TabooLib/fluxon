package org.tabooproject.fluxon.runtime.error;

import org.tabooproject.fluxon.runtime.FluxonRuntimeError;

/**
 * 没有找到对应的 Evaluator
 */
public class EvaluatorNotFoundError extends FluxonRuntimeError {

    public EvaluatorNotFoundError(String message) {
        super(message);
    }
}
