package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.NullLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class NullLiteralEvaluator extends ExpressionEvaluator<NullLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.NULL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, NullLiteral expr) {
        return null;
    }

    @Override
    public Type generateBytecode(NullLiteral result, CodeContext ctx, MethodVisitor mv) {
        mv.visitInsn(Opcodes.ACONST_NULL);
        return Type.OBJECT;
    }
}