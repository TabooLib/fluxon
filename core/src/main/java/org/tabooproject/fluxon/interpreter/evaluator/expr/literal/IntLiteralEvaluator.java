package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.IntLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class IntLiteralEvaluator extends ExpressionEvaluator<IntLiteral> {

    public static final Type TYPE = new Type(Integer.class);

    @Override
    public ExpressionType getType() {
        return ExpressionType.INT_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, IntLiteral expr) {
        return expr.getValue();
    }

    @Override
    public void generateBytecode(IntLiteral result, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "valueOf", "(I)" + TYPE, false);
    }
}