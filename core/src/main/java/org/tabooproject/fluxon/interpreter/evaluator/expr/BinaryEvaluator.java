package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.*;
import org.tabooproject.fluxon.parser.expression.literal.NullLiteral;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;
import org.tabooproject.fluxon.util.NumberOperations;

import static org.objectweb.asm.Opcodes.*;

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
            case MINUS:
                checkNumberOperands(left, right);
                return NumberOperations.subtractNumbers((Number) left, (Number) right);
            case DIVIDE:
                checkNumberOperands(left, right);
                return NumberOperations.divideNumbers((Number) left, (Number) right);
            case MULTIPLY:
                checkNumberOperands(left, right);
                return NumberOperations.multiplyNumbers((Number) left, (Number) right);
            case PLUS:
                if (left instanceof Number && right instanceof Number) {
                    return NumberOperations.addNumbers((Number) left, (Number) right);
                }
                if (left instanceof String || right instanceof String) {
                    return String.valueOf(left) + right;
                }
                throw new RuntimeException("Operands must be numbers or strings.");
            case MODULO:
                checkNumberOperands(left, right);
                return NumberOperations.moduloNumbers((Number) left, (Number) right);
            case GREATER:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers((Number) left, (Number) right) > 0;
            case GREATER_EQUAL:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers((Number) left, (Number) right) >= 0;
            case LESS:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers((Number) left, (Number) right) < 0;
            case LESS_EQUAL:
                checkNumberOperands(left, right);
                return NumberOperations.compareNumbers((Number) left, (Number) right) <= 0;
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
        // 生成左右操作数的字节码
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        Evaluator<ParseResult> leftEvaluator = registry.getEvaluator(expr.getLeft());
        Evaluator<ParseResult> rightEvaluator = registry.getEvaluator(expr.getRight());
        if (leftEvaluator == null || rightEvaluator == null) {
            throw new RuntimeException("No evaluator found for operands");
        }

        switch (expr.getOperator().getType()) {
            case PLUS:
                if (isStringConcatenation(expr)) {
                    generateStringConcatenation(expr, mv);
                } else {
                    leftEvaluator.generateBytecode(expr.getLeft(), mv);
                    rightEvaluator.generateBytecode(expr.getRight(), mv);
                    mv.visitInsn(IADD);
                }
                break;
            case MINUS:
                leftEvaluator.generateBytecode(expr.getLeft(), mv);
                rightEvaluator.generateBytecode(expr.getRight(), mv);
                mv.visitInsn(ISUB);
                break;
            case MULTIPLY:
                leftEvaluator.generateBytecode(expr.getLeft(), mv);
                rightEvaluator.generateBytecode(expr.getRight(), mv);
                mv.visitInsn(IMUL);
                break;
            case DIVIDE:
                leftEvaluator.generateBytecode(expr.getLeft(), mv);
                rightEvaluator.generateBytecode(expr.getRight(), mv);
                mv.visitInsn(IDIV);
                break;
            case MODULO:
                leftEvaluator.generateBytecode(expr.getLeft(), mv);
                rightEvaluator.generateBytecode(expr.getRight(), mv);
                mv.visitInsn(IREM);
                break;
            case EQUAL:
                if (isNullComparison(expr)) {
                    generateNullComparison(expr, leftEvaluator, rightEvaluator, mv, true);
                } else {
                    generateComparisonBytecode(expr, leftEvaluator, rightEvaluator, mv, IF_ICMPEQ);
                }
                break;
            case NOT_EQUAL:
                if (isNullComparison(expr)) {
                    generateNullComparison(expr, leftEvaluator, rightEvaluator, mv, false);
                } else {
                    generateComparisonBytecode(expr, leftEvaluator, rightEvaluator, mv, IF_ICMPNE);
                }
                break;
            case GREATER:
                generateComparisonBytecode(expr, leftEvaluator, rightEvaluator, mv, IF_ICMPGT);
                break;
            case GREATER_EQUAL:
                generateComparisonBytecode(expr, leftEvaluator, rightEvaluator, mv, IF_ICMPGE);
                break;
            case LESS:
                generateComparisonBytecode(expr, leftEvaluator, rightEvaluator, mv, IF_ICMPLT);
                break;
            case LESS_EQUAL:
                generateComparisonBytecode(expr, leftEvaluator, rightEvaluator, mv, IF_ICMPLE);
                break;
            default:
                throw new RuntimeException("Unknown binary operator: " + expr.getOperator().getType());
        }
    }

    private boolean isStringConcatenation(BinaryExpression expr) {
        return expr.getLeft() instanceof StringLiteral ||
                expr.getRight() instanceof StringLiteral;
    }

    private boolean isNullComparison(BinaryExpression expr) {
        return expr.getLeft() instanceof NullLiteral ||
                expr.getRight() instanceof NullLiteral;
    }

    private void generateStringConcatenation(BinaryExpression expr, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        ParseResult left = expr.getLeft();
        ParseResult right = expr.getRight();
        Evaluator<ParseResult> leftEval = registry.getEvaluator(left);
        Evaluator<ParseResult> rightEval = registry.getEvaluator(right);

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

    private void generateNullComparison(
            BinaryExpression expr,
            Evaluator<ParseResult> leftEval,
            Evaluator<ParseResult> rightEval,
            MethodVisitor mv,
            boolean isEqual) {
        Label trueLabel = new Label();
        Label endLabel = new Label();

        leftEval.generateBytecode(expr.getLeft(), mv);
        rightEval.generateBytecode(expr.getRight(), mv);

        // 比较两个引用是否相等
        mv.visitJumpInsn(isEqual ? IF_ACMPEQ : IF_ACMPNE, trueLabel);

        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);

        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);

        mv.visitLabel(endLabel);
    }
}
