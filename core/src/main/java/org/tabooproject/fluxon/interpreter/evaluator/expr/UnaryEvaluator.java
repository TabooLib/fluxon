package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.UnaryExpression;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class UnaryEvaluator extends ExpressionEvaluator<UnaryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.UNARY;
    }

    @Override
    public Object evaluate(Interpreter interpreter, UnaryExpression result) {
        Object right = interpreter.evaluate(result.getRight());
        switch (result.getOperator().getType()) {
            case NOT:
                return !isTrue(right);
            case MINUS:
                checkNumberOperand(right);
                return negateNumber((Number) right);
            default:
                throw new RuntimeException("Unknown unary operator: " + result.getOperator().getType());
        }
    }

    @Override
    public Type generateBytecode(UnaryExpression result, CodeContext ctx, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        Evaluator<ParseResult> rightEval = registry.getEvaluator(result.getRight());
        if (rightEval == null) {
            throw new RuntimeException("No evaluator found for operand");
        }
        // 压入操作数
        rightEval.generateBytecode(result.getRight(), ctx, mv);
        // 判断操作数类型
        switch (result.getOperator().getType()) {
            case NOT:
                mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "isTrue", "(" + Type.OBJECT + ")Z", false);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(IXOR);
                return boxing(Type.Z, mv);
            case MINUS:
                mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "negateNumber", "(" + Type.NUMBER + ")" + Type.NUMBER, false);
                return Type.NUMBER;
            default:
                throw new RuntimeException("Unknown unary operator: " + result.getOperator().getType());
        }
    }
}
