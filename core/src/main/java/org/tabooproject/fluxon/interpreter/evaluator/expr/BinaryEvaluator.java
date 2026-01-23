package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class BinaryEvaluator extends ExpressionEvaluator<BinaryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.BINARY;
    }

    @Override
    public Type evaluate(Interpreter interpreter, BinaryExpression result) {
        Type lt = interpreter.evaluate(result.getLeft());
        Object left = interpreter.getResultBoxed(lt);
        Type rt = interpreter.evaluate(result.getRight());
        Object right = interpreter.getResultBoxed(rt);
        // @formatter:off
        switch (result.getOperator().getType()) {
            case PLUS:          interpreter.resultRef = add(left, right); break;
            case MINUS:         interpreter.resultRef = subtract(left, right); break;
            case DIVIDE:        interpreter.resultRef = divide(left, right); break;
            case MULTIPLY:      interpreter.resultRef = multiply(left, right); break;
            case MODULO:        interpreter.resultRef = modulo(left, right); break;
            case GREATER:       interpreter.resultRef = isGreater(left, right); break;
            case GREATER_EQUAL: interpreter.resultRef = isGreaterEqual(left, right); break;
            case LESS:          interpreter.resultRef = isLess(left, right); break;
            case LESS_EQUAL:    interpreter.resultRef = isLessEqual(left, right); break;
            case EQUAL:         interpreter.resultRef = isEqual(left, right); break;
            case NOT_EQUAL:     interpreter.resultRef = !isEqual(left, right); break;
            case IDENTICAL:     interpreter.resultRef = left == right; break;
            case NOT_IDENTICAL: interpreter.resultRef = left != right; break;
            default:            throw new RuntimeException("Unknown binary operator: " + result.getOperator().getType());
        }
        // @formatter:on
        switch (result.getOperator().getType()) {
            case GREATER: case GREATER_EQUAL: case LESS: case LESS_EQUAL:
            case EQUAL: case NOT_EQUAL: case IDENTICAL: case NOT_IDENTICAL:
                return Type.BOOLEAN;
            default:
                return Type.OBJECT;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Type generateBytecode(BinaryExpression expr, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> leftEval = ctx.getEvaluator(expr.getLeft());
        Evaluator<ParseResult> rightEval = ctx.getEvaluator(expr.getRight());
        if (leftEval == null || rightEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for operands");
        }
        // 特殊处理引用比较运算符（=== 和 !==）
        TokenType opType = expr.getOperator().getType();
        if (opType == TokenType.IDENTICAL || opType == TokenType.NOT_IDENTICAL) {
            return generateIdentityComparison(expr, leftEval, rightEval, ctx, mv, opType == TokenType.NOT_IDENTICAL);
        }
        // 获取 Operations 方法
        BinaryOperator operator = OPERATORS.get(opType);
        if (operator == null) {
            throw new RuntimeException("No operator found for binary expression");
        }
        // 生成字节码
        generateOperator(expr, leftEval, rightEval, operator.name, operator.descriptor, ctx, mv, operator.xor);
        // 将结果装箱
        return boxing(operator.type, mv);
    }

    /**
     * 生成引用比较字节码（=== 和 !==）
     */
    private Type generateIdentityComparison(
            BinaryExpression expr,
            Evaluator<ParseResult> leftEval,
            Evaluator<ParseResult> rightEval,
            CodeContext ctx,
            MethodVisitor mv,
            boolean negate
    ) {
        // 生成左右操作数的字节码
        Type lt = leftEval.generateBytecode(expr.getLeft(), ctx, mv);
        if (lt == Type.VOID) {
            throw new VoidError("Void type is not allowed for binary expression left operand");
        }
        boxing(lt, mv);
        Type rt = rightEval.generateBytecode(expr.getRight(), ctx, mv);
        if (rt == Type.VOID) {
            throw new VoidError("Void type is not allowed for binary expression right operand");
        }
        boxing(rt, mv);
        // 生成引用比较：if (a == b) push true else push false
        Label trueLabel = new Label();
        Label endLabel = new Label();
        mv.visitJumpInsn(negate ? IF_ACMPNE : IF_ACMPEQ, trueLabel);
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(endLabel);
        // 装箱为 Boolean
        return boxing(Type.Z, mv);
    }

    /**
     * 生成 Operations 方法字节码
     */
    private void generateOperator(
            BinaryExpression expr,
            Evaluator<ParseResult> leftEval,
            Evaluator<ParseResult> rightEval,
            String method,
            String descriptor,
            CodeContext ctx,
            MethodVisitor mv,
            boolean xor
    ) {
        // 生成左右操作数的字节码
        Type lt = leftEval.generateBytecode(expr.getLeft(), ctx, mv);
        if (lt == Type.VOID) {
            throw new VoidError("Void type is not allowed for binary expression left operand");
        }
        boxing(lt, mv);
        Type rt = rightEval.generateBytecode(expr.getRight(), ctx, mv);
        if (rt == Type.VOID) {
            throw new VoidError("Void type is not allowed for binary expression right operand");
        }
        boxing(rt, mv);
        // 调用 Operations 方法
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), method, descriptor, false);
        // 是否取反结果
        if (xor) {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IXOR);
        }
    }

    private static final Map<TokenType, BinaryOperator> OPERATORS = new HashMap<>();

    static {
        // 算术运算符
        OPERATORS.put(TokenType.PLUS, new BinaryOperator("add", Type.OBJECT));
        OPERATORS.put(TokenType.MINUS, new BinaryOperator("subtract", Type.OBJECT));
        OPERATORS.put(TokenType.MULTIPLY, new BinaryOperator("multiply", Type.OBJECT));
        OPERATORS.put(TokenType.DIVIDE, new BinaryOperator("divide", Type.OBJECT));
        OPERATORS.put(TokenType.MODULO, new BinaryOperator("modulo", Type.OBJECT));
        // 比较运算符
        OPERATORS.put(TokenType.GREATER, new BinaryOperator("isGreater", Type.Z));
        OPERATORS.put(TokenType.GREATER_EQUAL, new BinaryOperator("isGreaterEqual", Type.Z));
        OPERATORS.put(TokenType.LESS, new BinaryOperator("isLess", Type.Z));
        OPERATORS.put(TokenType.LESS_EQUAL, new BinaryOperator("isLessEqual", Type.Z));
        OPERATORS.put(TokenType.EQUAL, new BinaryOperator("isEqual", Type.Z));
        OPERATORS.put(TokenType.NOT_EQUAL, new BinaryOperator("isEqual", Type.Z, true));
    }

    private static class BinaryOperator {

        private final String name;
        private final String descriptor;
        private final boolean xor;
        private final Type type;

        public BinaryOperator(String name, Type type) {
            this(name, type, false);
        }

        public BinaryOperator(String name, Type type, boolean xor) {
            this.name = name;
            this.descriptor = "(" + Type.OBJECT + Type.OBJECT + ")" + type;
            this.type = type;
            this.xor = xor;
        }
    }
}
