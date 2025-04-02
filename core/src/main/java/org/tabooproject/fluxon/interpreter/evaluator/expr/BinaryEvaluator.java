package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;

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
        // 获取 Operations 方法
        BinaryOperator operator = OPERATORS.get(expr.getOperator().getType());
        if (operator == null) {
            throw new RuntimeException("No operator found for binary expression");
        }
        // 生成字节码
        generateOperator(expr, leftEval, rightEval, operator.name, operator.descriptor, ctx, mv, operator.xor);
        return operator.type;
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
        boxing(leftEval.generateBytecode(expr.getLeft(), ctx, mv), mv);
        boxing(rightEval.generateBytecode(expr.getRight(), ctx, mv), mv);
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

        public String getName() {
            return name;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public Type getType() {
            return type;
        }

        public boolean isXor() {
            return xor;
        }
    }
}
