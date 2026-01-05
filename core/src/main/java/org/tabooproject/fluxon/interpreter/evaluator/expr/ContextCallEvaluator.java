package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ContextCallExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * 上下文调用表达式求值器
 * 处理形如 "text" :: replace("a", "b") 或 "text" :: { replace("a", "b"); length } 的表达式
 */
public class ContextCallEvaluator extends ExpressionEvaluator<ContextCallExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.CONTEXT_CALL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ContextCallExpression expression) {
        // 获取之前的目标
        Object before = interpreter.getEnvironment().getTarget();
        // 覆盖目标
        interpreter.getEnvironment().setTarget(interpreter.evaluate(expression.getTarget()));
        try {
            return interpreter.evaluate(expression.getContext());
        } finally {
            // 恢复目标
            interpreter.getEnvironment().setTarget(before);
        }
    }

    @Override
    public Type generateBytecode(ContextCallExpression expression, CodeContext ctx, MethodVisitor mv) {
        // 获取目标表达式的求值器
        Evaluator<ParseResult> targetEval = ctx.getEvaluator(expression.getTarget());
        if (targetEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for target expression");
        }
        // 获取上下文表达式的求值器
        Evaluator<ParseResult> contextEval = ctx.getEvaluator(expression.getContext());
        if (contextEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for context expression");
        }

        // 首先保存当前的 target - 调用 environment.getTarget()
        BytecodeUtils.loadEnvironment(mv, ctx);
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getTarget", "()" + Type.OBJECT, false);
        int oldTargetIndex = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, oldTargetIndex);

        // 计算新的目标值
        Type targetType = targetEval.generateBytecode(expression.getTarget(), ctx, mv);
        if (targetType == Type.VOID) {
            throw new VoidError("Void type is not allowed for context call target");
        }

        // 设置新的 target - 调用 environment.setTarget(newTarget)
        int newTargetIndex = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, newTargetIndex); // 保存新 target
        BytecodeUtils.loadEnvironment(mv, ctx);
        mv.visitVarInsn(ALOAD, newTargetIndex); // 加载新 target
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setTarget", "(" + Type.OBJECT + ")V", false);

        // 在新环境中求值上下文表达式
        Type resultType = contextEval.generateBytecode(expression.getContext(), ctx, mv);

        // 处理结果（如果不是 void）
        int resultIndex = -1;
        if (resultType != Type.VOID) {
            resultIndex = ctx.allocateLocalVar(Type.OBJECT);
            mv.visitVarInsn(ASTORE, resultIndex);
        }

        // 恢复原来的 target - 调用 environment.setTarget(oldTarget)
        BytecodeUtils.loadEnvironment(mv, ctx);
        mv.visitVarInsn(ALOAD, oldTargetIndex); // 加载原 target
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setTarget", "(" + Type.OBJECT + ")V", false);

        // 恢复结果到栈上（如果不是 void）
        if (resultType != Type.VOID) {
            mv.visitVarInsn(ALOAD, resultIndex);
        }
        return resultType;
    }
}