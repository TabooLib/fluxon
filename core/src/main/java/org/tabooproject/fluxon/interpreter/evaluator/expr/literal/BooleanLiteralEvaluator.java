package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.BooleanLiteral;

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
    public void generateBytecode(BooleanLiteral result, MethodVisitor mv) {
        // 压入boolean值(使用int 1或0表示)
        mv.visitInsn(result.getValue() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
    }
}