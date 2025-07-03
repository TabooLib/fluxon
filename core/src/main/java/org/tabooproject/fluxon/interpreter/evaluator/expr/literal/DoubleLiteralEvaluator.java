package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.DoubleLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class DoubleLiteralEvaluator extends ExpressionEvaluator<DoubleLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.DOUBLE_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, DoubleLiteral expr) {
        return expr.getValue();
    }

    @Override
    public Type generateBytecode(DoubleLiteral result, CodeContext ctx, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        return boxing(Type.D, mv);
    }
}