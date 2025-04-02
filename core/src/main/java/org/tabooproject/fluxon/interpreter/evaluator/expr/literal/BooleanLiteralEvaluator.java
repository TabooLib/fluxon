package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.BooleanLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class BooleanLiteralEvaluator extends ExpressionEvaluator<BooleanLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.BOOLEAN_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, BooleanLiteral expr) {
        return expr.getValue();
    }

    @Override
    public Type generateBytecode(BooleanLiteral result, MethodVisitor mv) {
        mv.visitInsn(result.getValue() ? ICONST_1 : ICONST_0);
        mv.visitMethodInsn(INVOKESTATIC, Type.BOOLEAN.getPath(), "valueOf", "(Z)" + Type.BOOLEAN, false);
        return Type.BOOLEAN;
    }
}