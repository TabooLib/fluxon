package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.Assignment;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class AssignmentEvaluator extends ExpressionEvaluator<Assignment> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ASSIGNMENT;
    }

    @Override
    public Object evaluate(Interpreter interpreter, Assignment result) {
        Object value = interpreter.evaluate(result.getValue());
        Environment environment = interpreter.getEnvironment();
        // 根据赋值操作符类型处理赋值
        if (result.getOperator().getType() == TokenType.ASSIGN) {
            environment.assign(result.getName(), value);
        } else {
            // 处理复合赋值
            Object current = environment.get(result.getName());
            // 根据操作符类型进行不同的复合赋值操作
            switch (result.getOperator().getType()) {
                case PLUS_ASSIGN:
                    value = add(current, value);
                    break;
                case MINUS_ASSIGN:
                    value = subtract(current, value);
                    break;
                case MULTIPLY_ASSIGN:
                    value = multiply(current, value);
                    break;
                case DIVIDE_ASSIGN:
                    value = divide(current, value);
                    break;
                case MODULO_ASSIGN:
                    value = modulo(current, value);
                    break;
                default:
                    throw new RuntimeException("Unknown compound assignment operator: " + result.getOperator().getType());
            }
            environment.assign(result.getName(), value);
        }
        // Assignment 操作没有返回值
        return null;
    }

    @Override
    public Type generateBytecode(Assignment result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> valueEval = ctx.getEvaluator(result.getValue());
        if (valueEval == null) {
            throw new RuntimeException("No evaluator found for value");
        }
        // 根据赋值操作符类型处理赋值
        TokenType type = result.getOperator().getType();
        if (type == TokenType.ASSIGN) {
            // 写入变量
            mv.visitVarInsn(Opcodes.ALOAD, 0);                       // this
            mv.visitLdcInsn(result.getName());                       // 变量名
            if (valueEval.generateBytecode(result.getValue(), ctx, mv) == Type.VOID) {
                throw new RuntimeException("Void type is not allowed for assignment value");
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "assign", ASSIGN, false);
        } else {
            // 压入变量名 -> 用于后续的写回操作
            mv.visitVarInsn(Opcodes.ALOAD, 0);  // this
            mv.visitLdcInsn(result.getName());  // 变量名

            // 复制栈顶的两个值用于进行操作
            mv.visitInsn(Opcodes.DUP2);
            mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "get", GET, false);
            // 执行操作
            if (valueEval.generateBytecode(result.getValue(), ctx, mv) == Type.VOID) {
                throw new RuntimeException("Void type is not allowed for assignment value");
            }

            // 执行操作并写回变量
            String name = OPERATORS.get(type);
            if (name == null) {
                throw new RuntimeException("Unknown compound assignment operator: " + type);
            }
            mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), name, "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT, false);
            mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "assign", ASSIGN, false);
        }
        // Assignment 操作没有返回值
        return Type.VOID;
    }

    private static final String ASSIGN = "(" + Type.STRING + Type.OBJECT + ")V";
    private static final String GET = "(" + Type.STRING + ")" + Type.OBJECT;

    private static final Map<TokenType, String> OPERATORS = new HashMap<>();

    static {
        OPERATORS.put(TokenType.PLUS_ASSIGN, "add");
        OPERATORS.put(TokenType.MINUS_ASSIGN, "subtract");
        OPERATORS.put(TokenType.MULTIPLY_ASSIGN, "multiply");
        OPERATORS.put(TokenType.DIVIDE_ASSIGN, "divide");
        OPERATORS.put(TokenType.MODULO_ASSIGN, "modulo");
    }
}
