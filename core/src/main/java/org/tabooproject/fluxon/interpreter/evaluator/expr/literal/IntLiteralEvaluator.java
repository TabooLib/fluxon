package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.IntLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class IntLiteralEvaluator extends ExpressionEvaluator<IntLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.INT_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, IntLiteral expr) {
        // 使用预装箱值以降低装箱开销
        return expr.getBoxedValue();
    }

    @Override
    public Type generateBytecode(IntLiteral result, CodeContext ctx, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        return boxing(Type.I, mv);
    }
}
