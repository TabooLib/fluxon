package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

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
            case GREATER:       return compare(left, right) > 0;
            case GREATER_EQUAL: return compare(left, right) >= 0;
            case LESS:          return compare(left, right) < 0;
            case LESS_EQUAL:    return compare(left, right) <= 0;
            case EQUAL:         return isEqual(left, right);
            case NOT_EQUAL:     return !isEqual(left, right);
            default:            throw new RuntimeException("Unknown binary operator: " + result.getOperator().getType());
        }
        // @formatter:on
    }

    @Override
    public void generateBytecode(BinaryExpression expr, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        Evaluator<ParseResult> leftEval = registry.getEvaluator(expr.getLeft());
        Evaluator<ParseResult> rightEval = registry.getEvaluator(expr.getRight());
        if (leftEval == null || rightEval == null) {
            throw new RuntimeException("No evaluator found for operands");
        }
        switch (expr.getOperator().getType()) {
            case PLUS:
                generateOperator(expr, leftEval, rightEval, "add", SIGNATURE_O2O, mv, false);
                break;
            case MINUS:
                generateOperator(expr, leftEval, rightEval, "subtract", SIGNATURE_O2O, mv, false);
                break;
            case MULTIPLY:
                generateOperator(expr, leftEval, rightEval, "multiply", SIGNATURE_O2O, mv, false);
                break;
            case DIVIDE:
                generateOperator(expr, leftEval, rightEval, "divide", SIGNATURE_O2O, mv, false);
                break;
            case MODULO:
                generateOperator(expr, leftEval, rightEval, "modulo", SIGNATURE_O2O, mv, false);
                break;
            case GREATER:
                generateComparisonOperator(expr, leftEval, rightEval, mv, IF_ICMPGT);
                break;
            case GREATER_EQUAL:
                generateComparisonOperator(expr, leftEval, rightEval, mv, IF_ICMPGE);
                break;
            case LESS:
                generateComparisonOperator(expr, leftEval, rightEval, mv, IF_ICMPLT);
                break;
            case LESS_EQUAL:
                generateComparisonOperator(expr, leftEval, rightEval, mv, IF_ICMPLE);
                break;
            case EQUAL:
                generateOperator(expr, leftEval, rightEval, "isEqual", SIGNATURE_O2Z, mv, false);
                break;
            case NOT_EQUAL:
                generateOperator(expr, leftEval, rightEval, "isEqual", SIGNATURE_O2Z, mv, true);
                break;
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
            MethodVisitor mv,
            boolean xor
    ) {
        // 生成左右操作数的字节码
        leftEval.generateBytecode(expr.getLeft(), mv);
        rightEval.generateBytecode(expr.getRight(), mv);
        // 调用 Operations 方法
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), method, signature, false);
        // 是否取反结果
        if (xor) {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IXOR);
        }
    }

    /**
     * 生成 Operations 比较方法字节码
     */
    private void generateComparisonOperator(
            BinaryExpression expr,
            Evaluator<ParseResult> leftEval,
            Evaluator<ParseResult> rightEval,
            MethodVisitor mv,
            int opcode
    ) {
        // 生成左右操作数的字节码
        leftEval.generateBytecode(expr.getLeft(), mv);
        rightEval.generateBytecode(expr.getRight(), mv);
        // 调用 Operations.compare 方法进行比较
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "compare", SIGNATURE_O2I, false);
        // 根据比较结果和操作符类型生成条件跳转指令
        Label trueLabel = new Label();
        Label endLabel = new Label();
        mv.visitInsn(ICONST_0);              // 加载常量 0 用于比较
        mv.visitJumpInsn(opcode, trueLabel); // 如果比较结果与 0 的关系满足条件，跳转到 trueLabel
        mv.visitInsn(ICONST_0);              // false
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);              // true
        mv.visitLabel(endLabel);
    }

    private static final String SIGNATURE_O2O = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String SIGNATURE_O2I = "(Ljava/lang/Object;Ljava/lang/Object;)I";
    private static final String SIGNATURE_O2Z = "(Ljava/lang/Object;Ljava/lang/Object;)Z";
}
