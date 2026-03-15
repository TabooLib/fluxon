package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ContextCallExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import static org.objectweb.asm.Opcodes.*;

/**
 * 上下文调用表达式求值器
 * 处理形如 "text" :: replace("a", "b") 或 "text" :: { replace("a", "b"); length } 的表达式
 * 支持安全上下文调用操作符 ?:: 用于 null 短路
 */
public class ContextCallEvaluator extends ExpressionEvaluator<ContextCallExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.CONTEXT_CALL;
    }

    @Override
    public Type evaluate(Interpreter interpreter, ContextCallExpression expression) {
        // 求值目标表达式
        Type tt = interpreter.evaluate(expression.getTarget());
        Object targetValue = interpreter.getResultBoxed(tt);
        // null 检查（安全调用）
        if (targetValue == null && expression.isSafe()) {
            interpreter.resultRef = null;
            return Type.OBJECT;
        }
        Environment env = interpreter.getEnvironment();
        Object before = env.getTarget();
        env.setTarget(targetValue);
        try {
            return interpreter.evaluate(expression.getContext());
        } finally {
            env.setTarget(before);
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
        int saved = ctx.getLocalVarIndex();
        Instructions.loadEnvironment(mv, ctx);
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getTarget", "()" + Type.OBJECT, false);
        int oldTargetIndex = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, oldTargetIndex);

        // 计算新的目标值
        Type targetType = targetEval.generateBytecode(expression.getTarget(), ctx, mv);
        if (targetType == Type.VOID) {
            throw new VoidError("Void type is not allowed for context call target");
        }
        boxing(targetType, mv);

        // 处理安全调用（?::）的 null 短路逻辑
        Label endLabel = null;
        Label notNullLabel;
        if (expression.isSafe()) {
            endLabel = new Label();
            notNullLabel = new Label();
            // 复制 target 引用用于 null 检查
            mv.visitInsn(DUP);
            // 检查是否为 null
            mv.visitJumpInsn(IFNONNULL, notNullLabel);
            // null 分支：弹出 target，恢复旧 target，压入 null 并跳转到结束
            mv.visitInsn(POP);
            // 恢复原来的 target
            Instructions.loadEnvironment(mv, ctx);
            mv.visitVarInsn(ALOAD, oldTargetIndex);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setTarget", "(" + Type.OBJECT + ")V", false);
            // 返回 null
            mv.visitInsn(ACONST_NULL);
            mv.visitJumpInsn(GOTO, endLabel);
            // 非 null 分支
            mv.visitLabel(notNullLabel);
        }

        // 设置新的 target - 调用 environment.setTarget(newTarget)
        int newTargetIndex = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, newTargetIndex); // 保存新 target
        Instructions.loadEnvironment(mv, ctx);
        mv.visitVarInsn(ALOAD, newTargetIndex); // 加载新 target
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setTarget", "(" + Type.OBJECT + ")V", false);

        // 在新环境中求值上下文表达式
        // 为字节码生成阶段也设置 target 类型，确保 FunctionCallEvaluator 能正确选择 handler
        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        if (analyzer != null) {
            Type targetTypeForBytecode = analyzer.inferType(expression.getTarget());
            analyzer.pushTargetType(targetTypeForBytecode);
        }
        Type resultType;
        try {
            resultType = contextEval.generateBytecode(expression.getContext(), ctx, mv);
        } finally {
            if (analyzer != null) {
                analyzer.popTargetType();
            }
        }
        // 安全调用时需要装箱，确保与 null 分支的栈帧类型一致
        if (endLabel != null && resultType.isPrimitive()) {
            boxing(resultType, mv);
            resultType = Type.OBJECT;
        }

        // 恢复原来的 target - 调用 environment.setTarget(oldTarget)
        // 结果在栈底，setTarget 不会影响它
        Instructions.loadEnvironment(mv, ctx);
        mv.visitVarInsn(ALOAD, oldTargetIndex);
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setTarget", "(" + Type.OBJECT + ")V", false);

        // 安全调用的结束标签
        if (endLabel != null) {
            mv.visitLabel(endLabel);
            ctx.restoreLocalVarIndex(saved);
            // 安全调用始终返回 OBJECT 类型（可能是 null）
            return Type.OBJECT;
        }
        ctx.restoreLocalVarIndex(saved);
        return resultType;
    }

    @Override
    public void analyzeTypes(ContextCallExpression expression, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(expression.getTarget());
        Type targetType = analyzer.inferType(expression.getTarget());
        analyzer.pushTargetType(targetType);
        try {
            analyzer.analyzeNode(expression.getContext());
        } finally {
            analyzer.popTargetType();
        }
    }

    @Override
    public Type inferResultType(ContextCallExpression expression, TypeAnalyzer analyzer) {
        // 上下文调用的结果类型就是 context 表达式的类型
        return analyzer.inferType(expression.getContext());
    }
}