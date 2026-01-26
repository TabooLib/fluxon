package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
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
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class AssignmentEvaluator extends ExpressionEvaluator<AssignExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ASSIGNMENT;
    }

    @Override
    public Type evaluate(Interpreter interpreter, AssignExpression result) {
        ParseResult target = result.getTarget();
        Type vt = interpreter.evaluate(result.getValue());
        Environment env = interpreter.getEnvironment();
        // 变量赋值
        if (target instanceof Identifier) {
            int position = result.getPosition();
            if (position >= 0) {
                // 使用当前环境的类型（作用域隔离）
                Type varType = env.getVariableType(position);
                if (result.getOperator().getType() != TokenType.ASSIGN) {
                    // 复合赋值
                    Object current, value = interpreter.getResultBoxed(vt);
                    switch (varType.getDescriptor().charAt(0)) {
                        case 'I':
                        case 'Z':
                            current = env.getLocalInt(position);
                            break;
                        case 'J':
                            current = env.getLocalLong(position);
                            break;
                        case 'F':
                            current = env.getLocalFloat(position);
                            break;
                        case 'D':
                            current = env.getLocalDouble(position);
                            break;
                        default:
                            current = env.getLocalRef(position);
                            break;
                    }
                    Object newValue = applyCompoundOperation(current, value, result.getOperator().getType());
                    switch (varType.getDescriptor().charAt(0)) {
                        case 'I':
                        case 'Z':
                            env.setLocalInt(position, ((Number) newValue).intValue());
                            break;
                        case 'J':
                            env.setLocalLong(position, ((Number) newValue).longValue());
                            break;
                        case 'F':
                            env.setLocalFloat(position, ((Number) newValue).floatValue());
                            break;
                        case 'D':
                            env.setLocalDouble(position, ((Number) newValue).doubleValue());
                            break;
                        default:
                            env.setLocalRef(position, newValue);
                            break;
                    }
                } else {
                    // 简单赋值：类型相同直接写，避免转换
                    if (vt == varType && vt.isPrimitive()) {
                        long bits = interpreter.resultPrimitive;
                        switch (vt.getDescriptor().charAt(0)) {
                            case 'I':
                            case 'Z':
                                env.setLocalInt(position, (int) bits);
                                break;
                            case 'J':
                                env.setLocalLong(position, bits);
                                break;
                            case 'F':
                                env.setLocalFloat(position, Float.intBitsToFloat((int) bits));
                                break;
                            case 'D':
                                env.setLocalDouble(position, Double.longBitsToDouble(bits));
                                break;
                        }
                    } else if (varType.isPrimitive()) {
                        Number num = vt.isPrimitive() ? (Number) interpreter.getResultBoxed(vt) : (Number) interpreter.resultRef;
                        switch (varType.getDescriptor().charAt(0)) {
                            case 'I':
                            case 'Z':
                                env.setLocalInt(position, num.intValue());
                                break;
                            case 'J':
                                env.setLocalLong(position, num.longValue());
                                break;
                            case 'F':
                                env.setLocalFloat(position, num.floatValue());
                                break;
                            case 'D':
                                env.setLocalDouble(position, num.doubleValue());
                                break;
                        }
                    } else {
                        env.setLocalRef(position, interpreter.getResultBoxed(vt));
                    }
                }
            } else {
                // 根变量赋值
                String name = ((Identifier) target).getValue();
                Object value = interpreter.getResultBoxed(vt);
                if (result.getOperator().getType() != TokenType.ASSIGN) {
                    value = applyCompoundOperation(env.getRootVariable(name), value, result.getOperator().getType());
                }
                env.setRootVariable(name, value);
            }
        }
        // 索引访问赋值
        else if (target instanceof IndexAccessExpression) {
            // 先捕获值，避免后续 evaluate 覆盖 interpreter 的结果槽
            Object value = interpreter.getResultBoxed(vt);
            IndexAccessExpression idx = (IndexAccessExpression) target;
            Type tt = interpreter.evaluate(idx.getTarget());
            Object container = interpreter.getResultBoxed(tt);
            List<ParseResult> indices = idx.getIndices();
            for (int i = 0; i < indices.size() - 1; i++) {
                Type it = interpreter.evaluate(indices.get(i));
                container = Intrinsics.getIndex(container, interpreter.getResultBoxed(it));
            }
            Type lit = interpreter.evaluate(indices.get(indices.size() - 1));
            Object lastIndex = interpreter.getResultBoxed(lit);
            if (result.getOperator().getType() != TokenType.ASSIGN) {
                value = applyCompoundOperation(Intrinsics.getIndex(container, lastIndex), value, result.getOperator().getType());
            }
            Intrinsics.setIndex(container, lastIndex, value);
        }
        interpreter.resultRef = null;
        return Type.VOID;
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

    @Override
    public Type generateBytecode(AssignExpression result, CodeContext ctx, MethodVisitor mv) {
        ParseResult target = result.getTarget();
        Evaluator<ParseResult> valueEval = ctx.getEvaluator(result.getValue());
        if (valueEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for value");
        }
        // 变量赋值
        if (target instanceof Identifier) {
            generateAssignOperation(result, valueEval, ((Identifier) target).getValue(), ctx, mv);
        }
        // 索引访问赋值
        else if (target instanceof IndexAccessExpression) {
            generateIndexAccessOperation(result, valueEval, (IndexAccessExpression) target, ctx, mv);
        }
        // Assignment 操作没有返回值
        return VOID;
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
        Type t = valueEval.generateBytecode(result.getValue(), ctx, mv);
        if (t == VOID) {
            throw new VoidError("Void type is not allowed for assignment value");
        }
        boxing(t, mv);
        // 执行操作
        String operatorName = OPERATORS.get(operatorType);
        if (operatorName == null) {
            throw new RuntimeException("Unknown compound assignment operator: " + operatorType);
        }
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), operatorName, "(" + OBJECT + OBJECT + ")" + OBJECT, false);
    }

    /**
     * 生成变量赋值的字节码
     */
    private void generateAssignOperation(
            AssignExpression result,
            Evaluator<ParseResult> valueEval,
            String name,
            CodeContext ctx,
            MethodVisitor mv
    ) {
        int position = result.getPosition();
        TokenType type = result.getOperator().getType();
        if (position >= 0) {
            Type varType = ctx.getVariableType(position);
            // 局部变量赋值
            if (type == TokenType.ASSIGN) {
                if (varType.isPrimitive()) {
                    // 基本类型：调用对应的 typed setter
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitLdcInsn(position);
                    Type vt = valueEval.generateBytecode(result.getValue(), ctx, mv);
                    if (vt == VOID) {
                        throw new VoidError("Void type is not allowed for assignment value");
                    }
                    emitConvert(vt, varType, mv);
                    ReferenceEvaluator.emitSetLocal(varType, mv);
                } else {
                    // 引用类型：存 localRefs
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitLdcInsn(position);
                    Type vt = valueEval.generateBytecode(result.getValue(), ctx, mv);
                    if (vt == VOID) {
                        throw new VoidError("Void type is not allowed for assignment value");
                    }
                    boxing(vt, mv);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", SET_LOCAL_REF, false);
                }
            } else {
                // 复合赋值：目前仍使用装箱方式，后续可优化
                if (varType.isPrimitive()) {
                    // 基本类型复合赋值：先准备好 env 和 position，再计算值
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitLdcInsn(position);
                    // 读取当前值
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitLdcInsn(position);
                    ReferenceEvaluator.emitGetLocal(varType, mv);
                    boxing(varType, mv);
                    generateCompoundOperation(result, valueEval, type, ctx, mv);
                    // 结果是 Object，需要拆箱为目标类型
                    emitUnbox(varType, mv);
                    ReferenceEvaluator.emitSetLocal(varType, mv);
                } else {
                    // 引用类型复合赋值
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(position);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getLocalRef", GET_LOCAL_REF, false);
                    generateCompoundOperation(result, valueEval, type, ctx, mv);
                    mv.visitLdcInsn(position);
                    mv.visitInsn(SWAP);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", SET_LOCAL_REF, false);
                }
            }
        } else {
            // 根变量赋值 -> env.setRootVariable(name, value)
            if (type == TokenType.ASSIGN) {
                Instructions.loadEnvironment(mv, ctx);
                mv.visitLdcInsn(name);
                Type vt = valueEval.generateBytecode(result.getValue(), ctx, mv);
                if (vt == VOID) {
                    throw new VoidError("Void type is not allowed for assignment value");
                }
                boxing(vt, mv);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setRootVariable", SET_ROOT_VARIABLE, false);
            } else {
                // 复合赋值: env.setRootVariable(name, op(env.getRootVariable(name), newValue))
                Instructions.loadEnvironment(mv, ctx);
                mv.visitInsn(DUP);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getRootVariable", GET_ROOT_VARIABLE, false);
                generateCompoundOperation(result, valueEval, type, ctx, mv);
                mv.visitLdcInsn(name);
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setRootVariable", SET_ROOT_VARIABLE, false);
            }
        }
    }

    /**
     * 类型转换
     */
    private void emitConvert(Type from, Type to, MethodVisitor mv) {
        if (from.equals(to)) return;
        // 如果源类型是 OBJECT，先拆箱
        if (!from.isPrimitive()) {
            emitUnbox(to, mv);
            return;
        }
        // 基本类型之间的转换
        if (from == I) {
            if (to == J) mv.visitInsn(I2L);
            else if (to == F) mv.visitInsn(I2F);
            else if (to == D) mv.visitInsn(I2D);
        } else if (from == J) {
            if (to == I) mv.visitInsn(L2I);
            else if (to == F) mv.visitInsn(L2F);
            else if (to == D) mv.visitInsn(L2D);
        } else if (from == F) {
            if (to == I) mv.visitInsn(F2I);
            else if (to == J) mv.visitInsn(F2L);
            else if (to == D) mv.visitInsn(F2D);
        } else if (from == D) {
            if (to == I) mv.visitInsn(D2I);
            else if (to == J) mv.visitInsn(D2L);
            else if (to == F) mv.visitInsn(D2F);
        }
        // Z (boolean) 不需要转换，直接当 int 用
    }

    /**
     * 生成索引访问赋值的字节码
     */
    private void generateIndexAccessOperation(
            AssignExpression result,
            Evaluator<ParseResult> valueEval,
            IndexAccessExpression idx,
            CodeContext ctx,
            MethodVisitor mv
    ) {
        List<ParseResult> indices = idx.getIndices();
        TokenType operatorType = result.getOperator().getType();
        // 生成 target 的字节码
        Evaluator<ParseResult> targetEval = ctx.getEvaluator(idx.getTarget());
        if (targetEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for index access target");
        }
        Type tt = targetEval.generateBytecode(idx.getTarget(), ctx, mv);
        if (tt == VOID) {
            throw new VoidError("Void type is not allowed for index access target");
        }
        boxing(tt, mv);
        // 处理多索引：前 n-1 个索引用于导航到目标容器
        for (int i = 0; i < indices.size() - 1; i++) {
            Evaluator<ParseResult> indexEval = ctx.getEvaluator(indices.get(i));
            if (indexEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for index expression");
            }
            Type it = indexEval.generateBytecode(indices.get(i), ctx, mv);
            if (it == VOID) {
                throw new VoidError("Void type is not allowed for index");
            }
            boxing(it, mv);
            // 调用 Intrinsics.getIndex 导航到下一层
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "getIndex", "(" + OBJECT + OBJECT + ")" + OBJECT, false);
        }
        // 最后一个索引用于赋值
        ParseResult lastIndexExpr = indices.get(indices.size() - 1);
        Evaluator<ParseResult> lastIndexEval = ctx.getEvaluator(lastIndexExpr);
        if (lastIndexEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for last index expression");
        }
        // 简单赋值：container[index] = value
        if (operatorType == TokenType.ASSIGN) {
            Type lit = lastIndexEval.generateBytecode(lastIndexExpr, ctx, mv);
            if (lit == VOID) {
                throw new VoidError("Void type is not allowed for index");
            }
            boxing(lit, mv);
            // 栈：container, index
            Type vt = valueEval.generateBytecode(result.getValue(), ctx, mv);
            if (vt == VOID) {
                throw new VoidError("Void type is not allowed for assignment value");
            }
            boxing(vt, mv);
            // 栈：container, index, value
            // 调用 Intrinsics.setIndex(container, index, value)
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "setIndex", "(" + OBJECT + OBJECT + OBJECT + ")" + VOID, false);
            // 栈：（空）
        }
        // 复合赋值：container[index] += value
        // 需要先读取当前值，执行操作，再写回
        else {
            // 栈：container
            // 复制容器引用（用于后续的 setIndex）
            mv.visitInsn(DUP);
            // 栈：container, container

            Type lit = lastIndexEval.generateBytecode(lastIndexExpr, ctx, mv);
            if (lit == VOID) {
                throw new VoidError("Void type is not allowed for index");
            }
            boxing(lit, mv);
            // 栈：container, container, index

            // 使用 DUP_X1 复制索引到第二个位置
            // 这样可以保持 container 和 index 的副本在栈底
            mv.visitInsn(DUP_X1);
            // 栈：container, index, container, index

            // 获取当前值 getIndex(container, index)
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "getIndex", "(" + OBJECT + OBJECT + ")" + OBJECT, false);
            // 栈：container, index, currentValue

            // 执行复合操作，生成新值并调用操作
            generateCompoundOperation(result, valueEval, operatorType, ctx, mv);
            // 栈：container, index, resultValue

            // 调用 setIndex(container, index, resultValue)
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "setIndex", "(" + OBJECT + OBJECT + OBJECT + ")" + VOID, false);
            // 栈：（空）
        }
    }

    private static final String SET_LOCAL_REF = "(" + I + OBJECT + ")" + VOID;
    private static final String GET_LOCAL_REF = "(" + I + ")" + OBJECT;
    private static final String SET_ROOT_VARIABLE = "(" + STRING + OBJECT + ")" + VOID;
    private static final String GET_ROOT_VARIABLE = "(" + STRING + ")" + OBJECT;

    private static final Map<TokenType, String> OPERATORS = new HashMap<>();

    static {
        OPERATORS.put(TokenType.PLUS_ASSIGN, "add");
        OPERATORS.put(TokenType.MINUS_ASSIGN, "subtract");
        OPERATORS.put(TokenType.MULTIPLY_ASSIGN, "multiply");
        OPERATORS.put(TokenType.DIVIDE_ASSIGN, "divide");
        OPERATORS.put(TokenType.MODULO_ASSIGN, "modulo");
    }

    @Override
    public void analyzeTypes(AssignExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getValue());
        int position = result.getPosition();
        if (position < 0) return;
        Type valueType;
        if (result.getOperator().getType() != TokenType.ASSIGN) {
            Type currentType = analyzer.getVariableType(position);
            Type rightType = analyzer.inferType(result.getValue());
            valueType = analyzer.inferBinaryResultType(currentType, rightType, result.getOperator().getType());
        } else {
            valueType = analyzer.inferType(result.getValue());
        }
        analyzer.recordType(position, valueType);
    }

    @Override
    public Type inferResultType(AssignExpression result, TypeAnalyzer analyzer) {
        return analyzer.inferType(result.getValue());
    }
}
