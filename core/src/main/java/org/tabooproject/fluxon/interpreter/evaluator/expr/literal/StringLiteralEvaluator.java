package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;
import org.tabooproject.fluxon.runtime.Type;

public class StringLiteralEvaluator extends ExpressionEvaluator<StringLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.STRING_LITERAL;
    }

    @Override
    public Type evaluate(Interpreter interpreter, StringLiteral expr) {
        interpreter.resultRef = expr.getValue();
        return Type.STRING;
    }

    @Override
    public Type generateBytecode(StringLiteral result, CodeContext ctx, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        return Type.STRING;
    }
}