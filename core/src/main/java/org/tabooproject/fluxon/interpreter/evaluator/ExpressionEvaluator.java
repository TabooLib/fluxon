package org.tabooproject.fluxon.interpreter.evaluator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.BreakException;
import org.tabooproject.fluxon.interpreter.ContinueException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("DuplicatedCode")
public abstract class ExpressionEvaluator<T extends Expression> extends Evaluator<T> {

    /**
     * 表达式类型
     */
    abstract public ExpressionType getType();

    @Override
    public Type generateBytecode(T result, CodeContext ctx, MethodVisitor mv) {
        return Type.VOID;
    }

    public static void generateCondition(
            @NotNull CodeContext ctx,
            @NotNull MethodVisitor mv,
            @NotNull ParseResult condition,
            @NotNull Evaluator<ParseResult> conditionEval,
            @Nullable Label endLabel
    ) {
        // 评估条件表达式
        Type conditionType = conditionEval.generateBytecode(condition, ctx, mv);
        if (conditionType == Type.VOID) {
            throw new VoidError("Void type is not allowed for condition expression");
        }
        // 如果是装箱的 Boolean 类型，直接拆箱
        if (conditionType == Type.BOOLEAN) {
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.BOOLEAN.getPath(), "booleanValue", "()Z", false);
        }
        // 如果条件结果不是 boolean 类型，才调用 Operations.isTrue 判断条件
        else if (conditionType != Type.Z) {
            if (conditionType.isPrimitive()) {
                boxing(conditionType, mv);
            }
            mv.visitMethodInsn(INVOKESTATIC, Operations.TYPE.getPath(), "isTrue", "(" + Type.OBJECT + ")Z", false);
        }
        if (endLabel != null) {
            // 如果条件为假，跳转到结束
            mv.visitJumpInsn(IFEQ, endLabel);
        }
    }

    /**
     * 统一两个分支的类型（用于 if-then-else、三元运算符等）
     */
    public static Type unifyBranchTypes(Type trueType, Type falseType) {
        // void 分支在表达式位置统一为 null，不能按 primitive void 分配局部槽位。
        if (trueType == Type.VOID || falseType == Type.VOID) return Type.OBJECT;
        if (trueType.isPrimitive() && trueType.equals(falseType)) {
            return trueType;
        }
        // 数值类型提升
        if (trueType.isPrimitive() && falseType.isPrimitive()) {
            if (trueType == Type.D || falseType == Type.D) return Type.D;
            if (trueType == Type.F || falseType == Type.F) return Type.F;
            if (trueType == Type.J || falseType == Type.J) return Type.J;
        }
        return Type.OBJECT;
    }

    /**
     * 推断分支统一类型（用于 TypeAnalyzer）
     */
    public static Type inferBranchType(ParseResult trueBranch, ParseResult falseBranch, TypeAnalyzer analyzer) {
        Type trueType = analyzer.inferType(trueBranch);
        Type falseType = falseBranch != null ? analyzer.inferType(falseBranch) : Type.OBJECT;
        return unifyBranchTypes(trueType, falseType);
    }

    /**
     * 生成安全访问的 null 短路字节码
     * 栈顶为 target 引用，若为 null 则弹出并压入 null 跳转到 endLabel
     *
     * @return endLabel，调用方需在逻辑末尾 visitLabel(endLabel)
     */
    public static Label emitNullShortCircuit(MethodVisitor mv) {
        Label endLabel = new Label();
        Label notNullLabel = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, notNullLabel);
        mv.visitInsn(POP);
        mv.visitInsn(ACONST_NULL);
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(notNullLabel);
        return endLabel;
    }

    public static int storeOpcode(Type type) {
        if (type == Type.I || type == Type.Z) return ISTORE;
        if (type == Type.J) return LSTORE;
        if (type == Type.F) return FSTORE;
        if (type == Type.D) return DSTORE;
        return ASTORE;
    }

    public static int loadOpcode(Type type) {
        if (type == Type.I || type == Type.Z) return ILOAD;
        if (type == Type.J) return LLOAD;
        if (type == Type.F) return FLOAD;
        if (type == Type.D) return DLOAD;
        return ALOAD;
    }

    /**
     * 原始类型转换
     */
    public static void emitConvertPrimitive(Type from, Type to, MethodVisitor mv) {
        if (from.equals(to)) return;
        if (!from.isPrimitive()) {
            emitUnbox(to, mv);
            return;
        }
        if (from == Type.I) {
            if (to == Type.J) mv.visitInsn(I2L);
            else if (to == Type.F) mv.visitInsn(I2F);
            else if (to == Type.D) mv.visitInsn(I2D);
        } else if (from == Type.J) {
            if (to == Type.I) mv.visitInsn(L2I);
            else if (to == Type.F) mv.visitInsn(L2F);
            else if (to == Type.D) mv.visitInsn(L2D);
        } else if (from == Type.F) {
            if (to == Type.I) mv.visitInsn(F2I);
            else if (to == Type.J) mv.visitInsn(F2L);
            else if (to == Type.D) mv.visitInsn(F2D);
        } else if (from == Type.D) {
            if (to == Type.I) mv.visitInsn(D2I);
            else if (to == Type.J) mv.visitInsn(D2L);
            else if (to == Type.F) mv.visitInsn(D2F);
        }
    }

    public static void emitUnbox(Type type, MethodVisitor mv) {
        if (type == Type.I) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
        } else if (type == Type.J) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
        } else if (type == Type.F) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
        } else if (type == Type.D) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
        } else if (type == Type.Z) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        }
    }

    /**
     * 存储分支结果到局部变量
     */
    public static void storeBranchResult(Type branchType, Type unifiedType, int storeId, MethodVisitor mv) {
        if (unifiedType.isPrimitive()) {
            emitConvertPrimitive(branchType, unifiedType, mv);
            mv.visitVarInsn(storeOpcode(unifiedType), storeId);
        } else {
            if (branchType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
            } else {
                boxing(branchType, mv);
            }
            mv.visitVarInsn(ASTORE, storeId);
        }
    }

    /**
     * 生成循环体结束代码（丢弃返回值、跳回开始、结束标签、退出上下文）
     */
    public static void finishLoopBody(Type bodyType, MethodVisitor mv, CodeContext ctx, Label loopStart, Label loopEnd) {
        if (bodyType != Type.VOID) {
            mv.visitInsn((bodyType == Type.J || bodyType == Type.D) ? POP2 : POP);
        }
        mv.visitJumpInsn(GOTO, loopStart);
        mv.visitLabel(loopEnd);
        ctx.exitLoop();
    }

    /**
     * 执行循环体（含 costStep + break/continue 处理）
     *
     * @return true 表示 break，调用方应退出循环
     */
    protected static boolean executeLoopBody(Interpreter interpreter, ParseResult body, boolean bodyIsStatement) {
        if (!bodyIsStatement) {
            interpreter.consumeCostStep();
        }
        try {
            interpreter.evaluate(body);
        } catch (ContinueException ignored) {
        } catch (BreakException ignored) {
            return true;
        }
        return interpreter.hasReturn;
    }
}
