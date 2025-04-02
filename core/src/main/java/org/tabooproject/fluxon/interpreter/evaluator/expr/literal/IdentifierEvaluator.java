package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.runtime.Type;

public class IdentifierEvaluator extends ExpressionEvaluator<Identifier> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.IDENTIFIER;
    }

    @Override
    public Object evaluate(Interpreter interpreter, Identifier expr) {
        return expr.getValue();
    }

    @Override
    public Type generateBytecode(Identifier result, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        return Type.STRING;
    }
}