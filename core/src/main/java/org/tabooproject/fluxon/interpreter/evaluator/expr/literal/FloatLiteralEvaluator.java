package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.FloatLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class FloatLiteralEvaluator extends ExpressionEvaluator<FloatLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FLOAT_LITERAL;
    }

    @Override
    public Type evaluate(Interpreter interpreter, FloatLiteral expr) {
        interpreter.resultPrimitive = Float.floatToRawIntBits(expr.getValue());
        return Type.F;
    }

    @Override
    public Type generateBytecode(FloatLiteral result, CodeContext ctx, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        return Type.F;
    }
}
