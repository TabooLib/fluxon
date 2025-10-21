package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.error.EvaluatorNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VoidValueException;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class AssignmentEvaluator extends ExpressionEvaluator<AssignExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ASSIGNMENT;
    }

    @Override
    public Object evaluate(Interpreter interpreter, AssignExpression result) {
        ParseResult target = result.getTarget();
        Object value = interpreter.evaluate(result.getValue());
        Environment environment = interpreter.getEnvironment();

        // 变量赋值
        if (target instanceof Identifier) {
            String name = ((Identifier) target).getValue();
            // 根据赋值操作符类型处理赋值
            if (result.getOperator().getType() == TokenType.ASSIGN) {
                environment.assign(name, value, result.getPosition());
            } else {
                // 处理复合赋值
                Object current = environment.get(name, result.getPosition());
                value = applyCompoundOperation(current, value, result.getOperator().getType());
                environment.assign(name, value, result.getPosition());
            }
        }
        // 索引访问赋值
        else if (target instanceof IndexAccessExpression) {
            IndexAccessExpression idx = (IndexAccessExpression) target;
            Object container = interpreter.evaluate(idx.getTarget());
            List<ParseResult> indices = idx.getIndices();

            // 处理多索引：map["k1", "k2"] = v 等价于 map["k1"]["k2"] = v
            // 前 n-1 个索引用于导航到目标容器
            for (int i = 0; i < indices.size() - 1; i++) {
                Object index = interpreter.evaluate(indices.get(i));
                container = getIndex(container, index);
            }

            // 最后一个索引用于赋值
            Object lastIndex = interpreter.evaluate(indices.get(indices.size() - 1));
            if (result.getOperator().getType() == TokenType.ASSIGN) {
                Intrinsics.setIndex(container, lastIndex, value);
            } else {
                // 复合赋值
                Object current = getIndex(container, lastIndex);
                value = applyCompoundOperation(current, value, result.getOperator().getType());
                Intrinsics.setIndex(container, lastIndex, value);
            }
        }
        // Assignment 操作没有返回值
        return null;
    }

    /**
     * 应用复合赋值操作
     */
    private Object applyCompoundOperation(Object current, Object value, TokenType operator) {
        switch (operator) {
            case PLUS_ASSIGN:
                return add(current, value);
            case MINUS_ASSIGN:
                return subtract(current, value);
            case MULTIPLY_ASSIGN:
                return multiply(current, value);
            case DIVIDE_ASSIGN:
                return divide(current, value);
            case MODULO_ASSIGN:
                return modulo(current, value);
            default:
                throw new RuntimeException("Unknown compound assignment operator: " + operator);
        }
    }

    /**
     * 获取索引访问的值
     */
    private Object getIndex(Object container, Object index) {
        if (container instanceof List) {
            return ((List<?>) container).get(((Number) index).intValue());
        } else if (container instanceof Map) {
            return ((Map<?, ?>) container).get(index);
        } else {
            throw new RuntimeException("Cannot index type: " + (container == null ? "null" : container.getClass().getName()));
        }
    }

    @Override
    public Type generateBytecode(AssignExpression result, CodeContext ctx, MethodVisitor mv) {
        ParseResult target = result.getTarget();
        Evaluator<ParseResult> valueEval = ctx.getEvaluator(result.getValue());
        if (valueEval == null) {
            throw new EvaluatorNotFoundException("No evaluator found for value");
        }

        // 变量赋值
        if (target instanceof Identifier) {
            String name = ((Identifier) target).getValue();
            TokenType type = result.getOperator().getType();

            if (type == TokenType.ASSIGN) {
                // 写入变量
                mv.visitVarInsn(ALOAD, 0);             // this
                mv.visitLdcInsn(name);                 // 变量名
                if (valueEval.generateBytecode(result.getValue(), ctx, mv) == Type.VOID) {
                    throw new VoidValueException("Void type is not allowed for assignment value");
                }
                // 压入 index 参数
                mv.visitLdcInsn(result.getPosition());
                mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "assign", ASSIGN, false);
            } else {
                // 压入变量名 -> 用于后续的写回操作
                mv.visitVarInsn(ALOAD, 0);           // this
                mv.visitLdcInsn(name);               // 变量名

                // 复制栈顶的两个值用于进行操作
                mv.visitInsn(DUP2);
                // 压入 index 参数
                mv.visitLdcInsn(result.getPosition());
                mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "get", GET, false);

                // 执行复合操作
                generateCompoundOperation(result, valueEval, type, ctx, mv);

                // 压入 index 参数
                mv.visitLdcInsn(result.getPosition());
                mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "assign", ASSIGN, false);
            }
        }
        // 索引访问赋值
        else if (target instanceof IndexAccessExpression) {
            IndexAccessExpression idx = (IndexAccessExpression) target;
            List<ParseResult> indices = idx.getIndices();
            TokenType operatorType = result.getOperator().getType();

            // 生成 target 的字节码
            Evaluator<ParseResult> targetEval = ctx.getEvaluator(idx.getTarget());
            if (targetEval == null) {
                throw new EvaluatorNotFoundException("No evaluator found for index access target");
            }
            if (targetEval.generateBytecode(idx.getTarget(), ctx, mv) == Type.VOID) {
                throw new VoidValueException("Void type is not allowed for index access target");
            }

            // 处理多索引：前 n-1 个索引用于导航到目标容器
            for (int i = 0; i < indices.size() - 1; i++) {
                Evaluator<ParseResult> indexEval = ctx.getEvaluator(indices.get(i));
                if (indexEval == null) {
                    throw new EvaluatorNotFoundException("No evaluator found for index expression");
                }
                if (indexEval.generateBytecode(indices.get(i), ctx, mv) == Type.VOID) {
                    throw new VoidValueException("Void type is not allowed for index");
                }
                // 调用 Intrinsics.getIndex 导航到下一层
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        Intrinsics.TYPE.getPath(),
                        "getIndex",
                        "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT,
                        false
                );
            }

            // 最后一个索引用于赋值
            ParseResult lastIndexExpr = indices.get(indices.size() - 1);
            Evaluator<ParseResult> lastIndexEval = ctx.getEvaluator(lastIndexExpr);
            if (lastIndexEval == null) {
                throw new EvaluatorNotFoundException("No evaluator found for last index expression");
            }

            // 简单赋值：container[index] = value
            if (operatorType == TokenType.ASSIGN) {
                if (lastIndexEval.generateBytecode(lastIndexExpr, ctx, mv) == Type.VOID) {
                    throw new VoidValueException("Void type is not allowed for index");
                }
                // 栈：container, index
                if (valueEval.generateBytecode(result.getValue(), ctx, mv) == Type.VOID) {
                    throw new VoidValueException("Void type is not allowed for assignment value");
                }
                // 栈：container, index, value
                // 调用 Intrinsics.setIndex(container, index, value)
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        Intrinsics.TYPE.getPath(),
                        "setIndex",
                        "(" + Type.OBJECT + Type.OBJECT + Type.OBJECT + ")" + Type.VOID,
                        false
                );
                // 栈：（空）
            }
            // 复合赋值：container[index] += value
            // 需要先读取当前值，执行操作，再写回
            else {
                // 栈：container
                // 复制容器引用（用于后续的 setIndex）
                mv.visitInsn(DUP);
                // 栈：container, container

                if (lastIndexEval.generateBytecode(lastIndexExpr, ctx, mv) == Type.VOID) {
                    throw new VoidValueException("Void type is not allowed for index");
                }
                // 栈：container, container, index

                // 使用 DUP_X1 复制索引到第二个位置
                // 这样可以保持 container 和 index 的副本在栈底
                mv.visitInsn(DUP_X1);
                // 栈：container, index, container, index

                // 获取当前值 getIndex(container, index)
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        Intrinsics.TYPE.getPath(),
                        "getIndex",
                        "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT,
                        false
                );
                // 栈：container, index, currentValue

                // 执行复合操作，生成新值并调用操作
                generateCompoundOperation(result, valueEval, operatorType, ctx, mv);
                // 栈：container, index, resultValue

                // 调用 setIndex(container, index, resultValue)
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        Intrinsics.TYPE.getPath(),
                        "setIndex",
                        "(" + Type.OBJECT + Type.OBJECT + Type.OBJECT + ")" + Type.VOID,
                        false
                );
                // 栈：（空）
            }
        }
        // Assignment 操作没有返回值
        return Type.VOID;
    }

    /**
     * 生成复合赋值操作的字节码
     * 栈输入：currentValue
     * 栈输出：resultValue
     */
    private void generateCompoundOperation(
            AssignExpression result,
            Evaluator<ParseResult> valueEval,
            TokenType operatorType,
            CodeContext ctx,
            MethodVisitor mv
    ) {
        // 生成新值的字节码
        if (valueEval.generateBytecode(result.getValue(), ctx, mv) == Type.VOID) {
            throw new VoidValueException("Void type is not allowed for assignment value");
        }
        // 执行操作
        String operatorName = OPERATORS.get(operatorType);
        if (operatorName == null) {
            throw new RuntimeException("Unknown compound assignment operator: " + operatorType);
        }
        mv.visitMethodInsn(
                INVOKESTATIC,
                TYPE.getPath(),
                operatorName,
                "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT,
                false
        );
    }

    private static final String ASSIGN = "(" + Type.STRING + Type.OBJECT + Type.I + ")" + Type.VOID;
    private static final String GET = "(" + Type.STRING + Type.I + ")" + Type.OBJECT;

    private static final Map<TokenType, String> OPERATORS = new HashMap<>();

    static {
        OPERATORS.put(TokenType.PLUS_ASSIGN, "add");
        OPERATORS.put(TokenType.MINUS_ASSIGN, "subtract");
        OPERATORS.put(TokenType.MULTIPLY_ASSIGN, "multiply");
        OPERATORS.put(TokenType.DIVIDE_ASSIGN, "divide");
        OPERATORS.put(TokenType.MODULO_ASSIGN, "modulo");
    }
}
