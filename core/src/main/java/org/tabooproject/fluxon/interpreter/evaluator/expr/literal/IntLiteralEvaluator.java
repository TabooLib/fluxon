package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
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
    public Type evaluate(Interpreter interpreter, IntLiteral expr) {
        interpreter.resultPrimitive = expr.getValue();
        return Type.I;
    }

    @Override
    public Type generateBytecode(IntLiteral result, CodeContext ctx, MethodVisitor mv) {
        mv.visitLdcInsn(result.getValue());
        return Type.I;
    }

    @Override
    public Type inferResultType(IntLiteral result, TypeAnalyzer analyzer) {
        return Type.I;
    }
}
