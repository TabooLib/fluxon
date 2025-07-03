package org.tabooproject.fluxon.interpreter.error;

/**
 * 没有找到对应的 Evaluator
 */
public class EvaluatorNotFoundException extends RuntimeException {

    public EvaluatorNotFoundException(String message) {
        super(message);
    }
}
