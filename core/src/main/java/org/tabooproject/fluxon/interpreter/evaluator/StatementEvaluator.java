package org.tabooproject.fluxon.interpreter.evaluator;

import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.parser.statement.StatementType;

public abstract class StatementEvaluator<T extends Statement> implements Evaluator<T> {

    /**
     * 语句类型
     */
    abstract public StatementType getType();
}
