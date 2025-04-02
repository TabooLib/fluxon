package org.tabooproject.fluxon.interpreter.evaluator;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

public abstract class StatementEvaluator<T extends Statement> implements Evaluator<T> {

    /**
     * 语句类型
     */
    abstract public StatementType getType();

    @Override
    public Type generateBytecode(T result, MethodVisitor mv) {
        return Type.OBJECT;
    }
}
