package org.tabooproject.fluxon.interpreter.evaluator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.WhileExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import static org.objectweb.asm.Opcodes.*;

public abstract class ExpressionEvaluator<T extends Expression> extends Evaluator<T> {

    /**
     * 表达式类型
     */
    abstract public ExpressionType getType();

    @Override
    public Type generateBytecode(T result, CodeContext ctx, MethodVisitor mv) {
        return Type.OBJECT;
    }

    protected static void generateCondition(
            @NotNull CodeContext ctx,
            @NotNull MethodVisitor mv,
            @NotNull ParseResult condition,
            @NotNull Evaluator<ParseResult> conditionEval,
            @Nullable Label endLabel
    ) {
        // 评估条件表达式
        Type conditionType = conditionEval.generateBytecode(condition, ctx, mv);
        // 如果是装箱的 Boolean 类型，直接拆箱
        if (conditionType == Type.BOOLEAN) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        }
        // 如果条件结果不是 boolean 类型，才调用 Operations.isTrue 判断条件
        else if (conditionType != Type.Z) {
            mv.visitMethodInsn(INVOKESTATIC, Operations.TYPE.getPath(), "isTrue", "(" + Type.OBJECT + ")Z", false);
        }
        if (endLabel != null) {
            // 如果条件为假，跳转到结束
            mv.visitJumpInsn(IFEQ, endLabel);
        }
    }
}
