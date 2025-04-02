package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.Assignment;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

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
        return value;
    }

    @Override
    public Type generateBytecode(Assignment result, CodeContext ctx, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        Evaluator<ParseResult> valueEval = registry.getEvaluator(result.getValue());
        if (valueEval == null) {
            throw new RuntimeException("No evaluator found for value");
        }
        // 根据赋值操作符类型处理赋值
        if (result.getOperator().getType() == TokenType.ASSIGN) {
            // 写入变量
            mv.visitVarInsn(Opcodes.ALOAD, 0);                                   // this
            mv.visitLdcInsn(result.getName());                                   // 变量名
            boxing(valueEval.generateBytecode(result.getValue(), ctx, mv), mv);  // 变量值
            mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "setVariable", SET_VARIABLE, false);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 0);  // this
            mv.visitLdcInsn(result.getName());  // 变量名

            // 再次压入变量用于进行操作
            mv.visitVarInsn(Opcodes.ALOAD, 0);  // this
            mv.visitLdcInsn(result.getName());  // 变量名
            mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "getVariable", GET_VARIABLE, false);
            // 压入操作值
            boxing(valueEval.generateBytecode(result.getValue(), ctx, mv), mv);

            // 根据操作符类型进行不同的复合赋值操作
            switch (result.getOperator().getType()) {
                case PLUS_ASSIGN:
                    mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "add", OPT, false);
                    break;
                case MINUS_ASSIGN:
                    mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "subtract", OPT, false);
                    break;
                case MULTIPLY_ASSIGN:
                    mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "multiply", OPT, false);
                    break;
                case DIVIDE_ASSIGN:
                    mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "divide", OPT, false);
                    break;
                case MODULO_ASSIGN:
                    mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "modulo", OPT, false);
                    break;
                default:
                    throw new RuntimeException("Unknown compound assignment operator: " + result.getOperator().getType());
            }

            // 写回变量
            mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "setVariable", SET_VARIABLE, false);
        }
        return Type.VOID;
    }

    private static final String SET_VARIABLE = "(" + Type.STRING + Type.OBJECT + ")V";
    private static final String GET_VARIABLE = "(" + Type.STRING + ")" + Type.OBJECT;
    private static final String OPT = "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT;
}
