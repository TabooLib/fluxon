package org.tabooproject.fluxon.interpreter.evaluator;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.parser.ParseResult;

public interface Evaluator<T extends ParseResult> {

    /**
     * 评估结果
     */
    Object evaluate(Interpreter interpreter, T result);
}
