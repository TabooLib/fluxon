package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.DoubleLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class DoubleLiteralEvaluator extends ExpressionEvaluator<DoubleLiteral> {

    public static final Type TYPE = new Type(Double.class);

    @Override
    public ExpressionType getType() {
        return ExpressionType.DOUBLE_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, DoubleLiteral expr) {
        return expr.getValue();
    }

    @Override
    public void generateBytecode(DoubleLiteral result, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "valueOf", "(D)" + TYPE, false);
    }
}