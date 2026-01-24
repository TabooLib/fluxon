package org.tabooproject.fluxon.interpreter.evaluator.expr.literal;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.literal.BooleanLiteral;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;

public class BooleanLiteralEvaluator extends ExpressionEvaluator<BooleanLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.BOOLEAN_LITERAL;
    }

    @Override
    public Type evaluate(Interpreter interpreter, BooleanLiteral expr) {
        interpreter.resultPrimitive = expr.getValue() ? 1 : 0;
        return Type.Z;
    }

    @Override
    public Type generateBytecode(BooleanLiteral result, CodeContext ctx, MethodVisitor mv) {
        mv.visitInsn(result.getValue() ? ICONST_1 : ICONST_0);
        return Type.Z;
    }

    @Override
    public Type inferResultType(BooleanLiteral result, TypeAnalyzer analyzer) {
        return Type.Z;
    }
}