package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class BinaryEvaluator extends ExpressionEvaluator<BinaryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.BINARY;
    }

    @Override
    public Object evaluate(Interpreter interpreter, BinaryExpression result) {
        Object left = interpreter.evaluate(result.getLeft());
        Object right = interpreter.evaluate(result.getRight());
        // @formatter:off
        switch (result.getOperator().getType()) {
            case PLUS:          return add(left, right);
            case MINUS:         return subtract(left, right);
            case DIVIDE:        return divide(left, right);
            case MULTIPLY:      return multiply(left, right);
            case MODULO:        return modulo(left, right);
            case GREATER:       return isGreater(left, right);
            case GREATER_EQUAL: return isGreaterEqual(left, right);
            case LESS:          return isLess(left, right);
            case LESS_EQUAL:    return isLessEqual(left, right);
            case EQUAL:         return isEqual(left, right);
            case NOT_EQUAL:     return !isEqual(left, right);
            default:            throw new RuntimeException("Unknown binary operator: " + result.getOperator().getType());
        }
        // @formatter:on
    }

    @Override
    public Type generateBytecode(BinaryExpression expr, CodeContext ctx, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        Evaluator<ParseResult> leftEval = registry.getEvaluator(expr.getLeft());
        Evaluator<ParseResult> rightEval = registry.getEvaluator(expr.getRight());
        if (leftEval == null || rightEval == null) {
            throw new RuntimeException("No evaluator found for operands");
        }
        switch (expr.getOperator().getType()) {
            case PLUS:
                generateOperator(expr, leftEval, rightEval, "add", SIGNATURE_O2O, ctx, mv, false);
                return Type.OBJECT;
            case MINUS:
                generateOperator(expr, leftEval, rightEval, "subtract", SIGNATURE_O2O, ctx, mv, false);
                return Type.OBJECT;
            case MULTIPLY:
                generateOperator(expr, leftEval, rightEval, "multiply", SIGNATURE_O2O, ctx, mv, false);
                return Type.OBJECT;
            case DIVIDE:
                generateOperator(expr, leftEval, rightEval, "divide", SIGNATURE_O2O, ctx, mv, false);
                return Type.OBJECT;
            case MODULO:
                generateOperator(expr, leftEval, rightEval, "modulo", SIGNATURE_O2O, ctx, mv, false);
                return Type.OBJECT;
            case GREATER:
                generateOperator(expr, leftEval, rightEval, "isGreater", SIGNATURE_O2Z, ctx, mv, false);
                return Type.Z;
            case GREATER_EQUAL:
                generateOperator(expr, leftEval, rightEval, "isGreaterEqual", SIGNATURE_O2Z, ctx, mv, false);
                return Type.Z;
            case LESS:
                generateOperator(expr, leftEval, rightEval, "isLess", SIGNATURE_O2Z, ctx, mv, false);
                return Type.Z;
            case LESS_EQUAL:
                generateOperator(expr, leftEval, rightEval, "isLessEqual", SIGNATURE_O2Z, ctx, mv, false);
                return Type.Z;
            case EQUAL:
                generateOperator(expr, leftEval, rightEval, "isEqual", SIGNATURE_O2Z, ctx, mv, false);
                return Type.Z;
            case NOT_EQUAL:
                generateOperator(expr, leftEval, rightEval, "isEqual", SIGNATURE_O2Z, ctx, mv, true);
                return Type.Z;
            default:
                throw new RuntimeException("Unknown binary operator: " + expr.getOperator().getType());
        }
    }

    /**
     * 生成 Operations 方法字节码
     */
    private void generateOperator(
            BinaryExpression expr,
            Evaluator<ParseResult> leftEval,
            Evaluator<ParseResult> rightEval,
            String method,
            String signature,
            CodeContext ctx,
            MethodVisitor mv,
            boolean xor
    ) {
        // 生成左右操作数的字节码
        boxing(leftEval.generateBytecode(expr.getLeft(), ctx, mv), mv);
        boxing(rightEval.generateBytecode(expr.getRight(), ctx, mv), mv);
        // 调用 Operations 方法
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), method, signature, false);
        // 是否取反结果
        if (xor) {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IXOR);
        }
    }

    private static final String SIGNATURE_O2O = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String SIGNATURE_O2Z = "(Ljava/lang/Object;Ljava/lang/Object;)Z";
}
