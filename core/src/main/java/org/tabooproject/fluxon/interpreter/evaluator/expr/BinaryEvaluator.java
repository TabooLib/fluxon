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
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;

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

        switch (result.getOperator().getType()) {
            case PLUS:
                return add(left, right);
            case MINUS:
                checkNumberOperands(left, right);
                return subtractNumbers((Number) left, (Number) right);
            case DIVIDE:
                checkNumberOperands(left, right);
                return divideNumbers((Number) left, (Number) right);
            case MULTIPLY:
                checkNumberOperands(left, right);
                return multiplyNumbers((Number) left, (Number) right);
            case MODULO:
                checkNumberOperands(left, right);
                return moduloNumbers((Number) left, (Number) right);
            case GREATER:
                checkNumberOperands(left, right);
                return compareNumbers((Number) left, (Number) right) > 0;
            case GREATER_EQUAL:
                checkNumberOperands(left, right);
                return compareNumbers((Number) left, (Number) right) >= 0;
            case LESS:
                checkNumberOperands(left, right);
                return compareNumbers((Number) left, (Number) right) < 0;
            case LESS_EQUAL:
                checkNumberOperands(left, right);
                return compareNumbers((Number) left, (Number) right) <= 0;
            case EQUAL:
                return isEqual(left, right);
            case NOT_EQUAL:
                return !isEqual(left, right);
            default:
                throw new RuntimeException("Unknown binary operator: " + result.getOperator().getType());
        }
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
                if (isStringConcatenation(expr)) {
                    generateStringConcatenation(expr, mv);
                } else {
                    leftEval.generateBytecode(expr.getLeft(), mv);
                    rightEval.generateBytecode(expr.getRight(), mv);
                    mv.visitInsn(IADD);
                }
                break;
            case MINUS:
                leftEval.generateBytecode(expr.getLeft(), mv);
                rightEval.generateBytecode(expr.getRight(), mv);
                mv.visitInsn(ISUB);
                break;
            case MULTIPLY:
                leftEval.generateBytecode(expr.getLeft(), mv);
                rightEval.generateBytecode(expr.getRight(), mv);
                mv.visitInsn(IMUL);
                break;
            case DIVIDE:
                leftEval.generateBytecode(expr.getLeft(), mv);
                rightEval.generateBytecode(expr.getRight(), mv);
                mv.visitInsn(IDIV);
                break;
            case MODULO:
                leftEval.generateBytecode(expr.getLeft(), mv);
                rightEval.generateBytecode(expr.getRight(), mv);
                mv.visitInsn(IREM);
                break;
            case GREATER:
                generateComparisonBytecode(expr, leftEval, rightEval, mv, IF_ICMPGT);
                break;
            case GREATER_EQUAL:
                generateComparisonBytecode(expr, leftEval, rightEval, mv, IF_ICMPGE);
                break;
            case LESS:
                generateComparisonBytecode(expr, leftEval, rightEval, mv, IF_ICMPLT);
                break;
            case LESS_EQUAL:
                generateComparisonBytecode(expr, leftEval, rightEval, mv, IF_ICMPLE);
                break;
            case EQUAL:
                generateEqualityComparison(expr, leftEval, rightEval, mv, true);
                break;
            case NOT_EQUAL:
                generateEqualityComparison(expr, leftEval, rightEval, mv, false);
                break;
            default:
                throw new RuntimeException("Unknown binary operator: " + expr.getOperator().getType());
        }
    }

    private boolean isStringConcatenation(BinaryExpression expr) {
        return expr.getLeft() instanceof StringLiteral || expr.getRight() instanceof StringLiteral;
    }

    private void generateStringConcatenation(BinaryExpression expr, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        ParseResult left = expr.getLeft();
        ParseResult right = expr.getRight();
        Evaluator<ParseResult> leftEval = registry.getEvaluator(left);
        Evaluator<ParseResult> rightEval = registry.getEvaluator(right);
        if (leftEval == null || rightEval == null) {
            throw new RuntimeException("No evaluator found for operands");
        }

        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "()V",
                false);

        leftEval.generateBytecode(left, mv);
        // append第一个值
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);

        rightEval.generateBytecode(right, mv);
        // append第二个值
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);

        // 转换为String
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
    }

    private void generateComparisonBytecode(
            BinaryExpression expr,
            Evaluator<ParseResult> leftEval,
            Evaluator<ParseResult> rightEval,
            MethodVisitor mv,
            int opcode) {
        leftEval.generateBytecode(expr.getLeft(), mv);
        rightEval.generateBytecode(expr.getRight(), mv);

        Label trueLabel = new Label();
        Label endLabel = new Label();

        // 比较指令，如果条件成立跳转到trueLabel
        mv.visitJumpInsn(opcode, trueLabel);

        // 条件不成立，压入false(0)
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);

        // 条件成立，压入true(1)
        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);

        mv.visitLabel(endLabel);
    }

    private void generateEqualityComparison(
            BinaryExpression expr,
            Evaluator<ParseResult> leftEval,
            Evaluator<ParseResult> rightEval,
            MethodVisitor mv,
            boolean isEqual) {
        Label trueLabel = new Label();
        Label endLabel = new Label();

        // 生成左右操作数的字节码
        leftEval.generateBytecode(expr.getLeft(), mv);
        rightEval.generateBytecode(expr.getRight(), mv);

        // 调用 Operations.isEqual 方法
        mv.visitMethodInsn(
                INVOKESTATIC,
                "org/tabooproject/fluxon/runtime/stdlib/Operations",
                "isEqual",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                false
        );

        // 如果是不等于操作符,需要取反结果
        if (!isEqual) {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IXOR);
        }

        mv.visitLabel(trueLabel);
        mv.visitLabel(endLabel);
    }
}
