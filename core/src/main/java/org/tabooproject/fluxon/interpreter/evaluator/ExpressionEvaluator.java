package org.tabooproject.fluxon.interpreter.evaluator;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;

public abstract class ExpressionEvaluator<T extends Expression> implements Evaluator<T> {

    /**
     * 表达式类型
     */
    abstract public ExpressionType getType();

    @Override
    public void generateBytecode(T result, MethodVisitor mv) {
    }
}
