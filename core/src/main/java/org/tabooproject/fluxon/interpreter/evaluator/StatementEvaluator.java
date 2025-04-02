package org.tabooproject.fluxon.interpreter.evaluator;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

public abstract class StatementEvaluator<T extends Statement> extends Evaluator<T> {

    /**
     * 语句类型
     */
    abstract public StatementType getType();

    @Override
    public Type generateBytecode(T result, CodeContext ctx, MethodVisitor mv) {
        return Type.OBJECT;
    }
}
