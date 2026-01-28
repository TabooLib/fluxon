package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
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
import org.tabooproject.fluxon.parser.expression.MemberAccessExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
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
                    Object current = getLocalBoxed(env, position, varType);
                    Object value = interpreter.getResultBoxed(vt);
                    Object newValue = applyCompoundOperation(current, value, result.getOperator().getType());
                    setLocalFromBoxed(env, position, varType, newValue);
                } else {
                    // 简单赋值：类型相同直接写，避免转换
                    if (vt == varType && vt.isPrimitive()) {
                        long bits = interpreter.resultPrimitive;
                        setLocalFromBits(env, position, vt, bits);
                    } else if (varType.isPrimitive()) {
                        Number num = vt.isPrimitive() ? (Number) interpreter.getResultBoxed(vt) : (Number) interpreter.resultRef;
                        setLocalFromBoxed(env, position, varType, num);
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
        // 成员访问赋值
        else if (target instanceof MemberAccessExpression) {
            Object value = interpreter.getResultBoxed(vt);
            MemberAccessExpression memberAccess = (MemberAccessExpression) target;
            Type tt = interpreter.evaluate(memberAccess.getTarget());
            Object targetObj = interpreter.getResultBoxed(tt);
            if (targetObj == null) {
                if (memberAccess.isSafe()) {
                    interpreter.resultRef = null;
                    return Type.VOID;
                }
                throw new NullPointerException("Cannot set field '" + memberAccess.getMemberName() + "' on null object");
            }
            String fieldName = memberAccess.getMemberName();
            try {
                if (result.getOperator().getType() != TokenType.ASSIGN) {
                    Object current = ReflectionHelper.getField(targetObj, fieldName);
                    value = applyCompoundOperation(current, value, result.getOperator().getType());
                }
                ReflectionHelper.setField(targetObj, fieldName, value);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        interpreter.resultRef = null;
        return Type.VOID;
    }

    /**
     * 读取局部变量的装箱值
     */
    private Object getLocalBoxed(Environment env, int position, Type varType) {
        switch (varType.getDescriptor().charAt(0)) {
            case 'I': case 'Z': return env.getLocalInt(position);
            case 'J': return env.getLocalLong(position);
            case 'F': return env.getLocalFloat(position);
            case 'D': return env.getLocalDouble(position);
            default: return env.getLocalRef(position);
        }
    }

    /**
     * 从 bits 写入基本类型到局部变量
     */
    private void setLocalFromBits(Environment env, int position, Type type, long bits) {
        switch (type.getDescriptor().charAt(0)) {
            case 'I': case 'Z': env.setLocalInt(position, (int) bits); break;
            case 'J': env.setLocalLong(position, bits); break;
            case 'F': env.setLocalFloat(position, Float.intBitsToFloat((int) bits)); break;
            case 'D': env.setLocalDouble(position, Double.longBitsToDouble(bits)); break;
        }
    }

    /**
     * 从装箱值写入局部变量
     */
    private void setLocalFromBoxed(Environment env, int position, Type varType, Object value) {
        switch (varType.getDescriptor().charAt(0)) {
            case 'I': case 'Z': env.setLocalInt(position, ((Number) value).intValue()); break;
            case 'J': env.setLocalLong(position, ((Number) value).longValue()); break;
            case 'F': env.setLocalFloat(position, ((Number) value).floatValue()); break;
            case 'D': env.setLocalDouble(position, ((Number) value).doubleValue()); break;
            default: env.setLocalRef(position, value); break;
        }
    }

    /**
     * 应用复合赋值操作
     */
    private Object applyCompoundOperation(Object current, Object value, TokenType operator) {
        switch (operator) {
            case PLUS_ASSIGN: return add(current, value);
            case MINUS_ASSIGN: return subtract(current, value);
            case MULTIPLY_ASSIGN: return multiply(current, value);
            case DIVIDE_ASSIGN: return divide(current, value);
            case MODULO_ASSIGN: return modulo(current, value);
            default: throw new RuntimeException("Unknown compound assignment operator: " + operator);
        }
    }

    @Override
    public Type generateBytecode(AssignExpression result, CodeContext ctx, MethodVisitor mv) {
        ParseResult target = result.getTarget();
        Evaluator<ParseResult> valueEval = requireEvaluator(ctx, result.getValue(), "value");
        // 变量赋值
        if (target instanceof Identifier) {
            generateAssignOperation(result, valueEval, ((Identifier) target).getValue(), ctx, mv);
        }
        // 索引访问赋值
        else if (target instanceof IndexAccessExpression) {
            generateIndexAccessOperation(result, valueEval, (IndexAccessExpression) target, ctx, mv);
        }
        // 成员访问赋值
        else if (target instanceof MemberAccessExpression) {
            generateMemberAccessOperation(result, valueEval, (MemberAccessExpression) target, ctx, mv);
        }
        // Assignment 操作没有返回值
        return VOID;
    }

    /**
     * 获取 evaluator，不存在时抛出错误
     */
    private Evaluator<ParseResult> requireEvaluator(CodeContext ctx, ParseResult expr, String context) {
        Evaluator<ParseResult> eval = ctx.getEvaluator(expr);
        if (eval == null) throw new EvaluatorNotFoundError("No evaluator found for " + context);
        return eval;
    }

    /**
     * 生成值的字节码并装箱，VOID 时抛出错误
     */
    private void generateBoxedValue(Evaluator<ParseResult> eval, ParseResult expr, CodeContext ctx, MethodVisitor mv) {
        Type t = eval.generateBytecode(expr, ctx, mv);
        if (t == VOID) throw new VoidError("Void type is not allowed for assignment value");
        boxing(t, mv);
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
        generateBoxedValue(valueEval, result.getValue(), ctx, mv);
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
                Instructions.loadEnvironment(mv, ctx);
                mv.visitLdcInsn(position);
                Type vt = valueEval.generateBytecode(result.getValue(), ctx, mv);
                if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
                if (varType.isPrimitive()) {
                    emitConvert(vt, varType, mv);
                    ReferenceEvaluator.emitSetLocal(varType, mv);
                } else {
                    boxing(vt, mv);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", SET_LOCAL_REF, false);
                }
            } else {
                // 复合赋值
                if (varType.isPrimitive()) {
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitLdcInsn(position);
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitLdcInsn(position);
                    ReferenceEvaluator.emitGetLocal(varType, mv);
                    boxing(varType, mv);
                    generateCompoundOperation(result, valueEval, type, ctx, mv);
                    emitUnbox(varType, mv);
                    ReferenceEvaluator.emitSetLocal(varType, mv);
                } else {
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
            // 根变量赋值
            Instructions.loadEnvironment(mv, ctx);
            if (type == TokenType.ASSIGN) {
                mv.visitLdcInsn(name);
                generateBoxedValue(valueEval, result.getValue(), ctx, mv);
            } else {
                mv.visitInsn(DUP);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getRootVariable", GET_ROOT_VARIABLE, false);
                generateCompoundOperation(result, valueEval, type, ctx, mv);
                mv.visitLdcInsn(name);
                mv.visitInsn(SWAP);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setRootVariable", SET_ROOT_VARIABLE, false);
        }
    }

    // 基本类型转换指令查表 [from][to]: I=0, J=1, F=2, D=3
    private static final int[][] CONVERT_INSNS = {
        //     I    J    F    D
        /* I */ {0, I2L, I2F, I2D},
        /* J */ {L2I, 0, L2F, L2D},
        /* F */ {F2I, F2L, 0, F2D},
        /* D */ {D2I, D2L, D2F, 0},
    };

    private static int typeIndex(Type t) {
        switch (t.getDescriptor().charAt(0)) {
            case 'I': case 'Z': return 0;
            case 'J': return 1;
            case 'F': return 2;
            case 'D': return 3;
            default: return -1;
        }
    }

    /**
     * 类型转换
     */
    private void emitConvert(Type from, Type to, MethodVisitor mv) {
        if (from.equals(to)) return;
        if (!from.isPrimitive()) {
            emitUnbox(to, mv);
            return;
        }
        int fi = typeIndex(from), ti = typeIndex(to);
        if (fi >= 0 && ti >= 0) {
            int insn = CONVERT_INSNS[fi][ti];
            if (insn != 0) mv.visitInsn(insn);
        }
    }

    /**
     * 生成成员访问赋值的字节码
     */
    private void generateMemberAccessOperation(
            AssignExpression result,
            Evaluator<ParseResult> valueEval,
            MemberAccessExpression memberAccess,
            CodeContext ctx,
            MethodVisitor mv
    ) {
        TokenType operatorType = result.getOperator().getType();
        String fieldName = memberAccess.getMemberName();
        boolean safe = memberAccess.isSafe();
        Evaluator<ParseResult> targetEval = requireEvaluator(ctx, memberAccess.getTarget(), "member access target");
        Type tt = targetEval.generateBytecode(memberAccess.getTarget(), ctx, mv);
        if (tt == VOID) throw new VoidError("Void type is not allowed for member access target");
        boxing(tt, mv);
        Label skipLabel = null;
        Label endLabel = null;
        if (safe) {
            skipLabel = new Label();
            endLabel = new Label();
            mv.visitInsn(DUP);
            mv.visitJumpInsn(IFNULL, skipLabel);
        }
        if (operatorType == TokenType.ASSIGN) {
            mv.visitLdcInsn(fieldName);
            generateBoxedValue(valueEval, result.getValue(), ctx, mv);
            mv.visitMethodInsn(INVOKESTATIC, ReflectionHelper.TYPE.getPath(), "setField", "(" + OBJECT + STRING + OBJECT + ")V", false);
        } else {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(fieldName);
            mv.visitMethodInsn(INVOKESTATIC, ReflectionHelper.TYPE.getPath(), "getField", "(" + OBJECT + STRING + ")" + OBJECT, false);
            generateCompoundOperation(result, valueEval, operatorType, ctx, mv);
            mv.visitLdcInsn(fieldName);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESTATIC, ReflectionHelper.TYPE.getPath(), "setField", "(" + OBJECT + STRING + OBJECT + ")V", false);
        }
        if (safe) {
            mv.visitJumpInsn(GOTO, endLabel);
            mv.visitLabel(skipLabel);
            mv.visitInsn(POP);
            mv.visitLabel(endLabel);
        }
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
        Evaluator<ParseResult> targetEval = requireEvaluator(ctx, idx.getTarget(), "index access target");
        Type tt = targetEval.generateBytecode(idx.getTarget(), ctx, mv);
        if (tt == VOID) throw new VoidError("Void type is not allowed for index access target");
        boxing(tt, mv);
        // 处理多索引：前 n-1 个索引用于导航到目标容器
        for (int i = 0; i < indices.size() - 1; i++) {
            generateBoxedValue(requireEvaluator(ctx, indices.get(i), "index expression"), indices.get(i), ctx, mv);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "getIndex", "(" + OBJECT + OBJECT + ")" + OBJECT, false);
        }
        // 最后一个索引用于赋值
        ParseResult lastIndexExpr = indices.get(indices.size() - 1);
        Evaluator<ParseResult> lastIndexEval = requireEvaluator(ctx, lastIndexExpr, "last index expression");
        if (operatorType == TokenType.ASSIGN) {
            generateBoxedValue(lastIndexEval, lastIndexExpr, ctx, mv);
            generateBoxedValue(valueEval, result.getValue(), ctx, mv);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "setIndex", "(" + OBJECT + OBJECT + OBJECT + ")" + VOID, false);
        } else {
            mv.visitInsn(DUP);
            generateBoxedValue(lastIndexEval, lastIndexExpr, ctx, mv);
            mv.visitInsn(DUP_X1);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "getIndex", "(" + OBJECT + OBJECT + ")" + OBJECT, false);
            generateCompoundOperation(result, valueEval, operatorType, ctx, mv);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "setIndex", "(" + OBJECT + OBJECT + OBJECT + ")" + VOID, false);
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
