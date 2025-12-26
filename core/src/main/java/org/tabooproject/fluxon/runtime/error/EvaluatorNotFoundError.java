package org.tabooproject.fluxon.runtime.error;

/**
 * 没有找到对应的 Evaluator
 */
public class EvaluatorNotFoundError extends FluxonRuntimeError {

    public EvaluatorNotFoundError(String message) {
        super(message);
    }
}
