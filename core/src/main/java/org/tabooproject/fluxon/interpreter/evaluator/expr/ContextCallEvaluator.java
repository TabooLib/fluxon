package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.error.EvaluatorNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VoidValueException;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ContextCall;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.ContextEnvironment;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * 上下文调用表达式求值器
 * 处理形如 "text" :: replace("a", "b") 或 "text" :: { replace("a", "b"); length } 的表达式
 */
public class ContextCallEvaluator extends ExpressionEvaluator<ContextCall> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.CONTEXT_CALL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ContextCall expression) {
        // 计算目标值
        Object target = interpreter.evaluate(expression.getTarget());
        // 创建上下文环境，将目标值绑定为 'this'
        ContextEnvironment contextEnv = new ContextEnvironment(interpreter.getEnvironment(), target);
        // 在上下文环境中求值右侧表达式
        return interpreter.executeWithEnvironment(expression.getContext(), contextEnv);
    }

    @Override
    public Type generateBytecode(ContextCall expression, CodeContext ctx, MethodVisitor mv) {
        // 获取目标表达式的求值器
        Evaluator<ParseResult> targetEval = ctx.getEvaluator(expression.getTarget());
        if (targetEval == null) {
            throw new EvaluatorNotFoundException("No evaluator found for target expression");
        }
        // 获取上下文表达式的求值器
        Evaluator<ParseResult> contextEval = ctx.getEvaluator(expression.getContext());
        if (contextEval == null) {
            throw new EvaluatorNotFoundException("No evaluator found for context expression");
        }

        // 分配必要的局部变量索引
        int oldEnvIndex = ctx.allocateLocalVar(Type.OBJECT);
        int targetIndex = ctx.allocateLocalVar(Type.OBJECT);

        // 保存当前环境
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        mv.visitVarInsn(ASTORE, oldEnvIndex);

        // 计算目标值
        Type targetType = targetEval.generateBytecode(expression.getTarget(), ctx, mv);
        if (targetType == Type.VOID) {
            throw new VoidValueException("Void type is not allowed for context call target");
        }
        mv.visitVarInsn(ASTORE, targetIndex);

        // 创建新的 ContextEnvironment
        mv.visitTypeInsn(NEW, ContextEnvironment.TYPE.getPath());
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, oldEnvIndex);  // 原环境
        mv.visitVarInsn(ALOAD, targetIndex);  // 目标对象
        mv.visitMethodInsn(
                INVOKESPECIAL,
                ContextEnvironment.TYPE.getPath(),
                "<init>",
                "(" + Environment.TYPE.getDescriptor() + Type.OBJECT.getDescriptor() + ")V", false);

        // 设置新环境
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitInsn(SWAP);
        mv.visitFieldInsn(PUTFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());

        // 在新环境中求值上下文表达式
        Type resultType = contextEval.generateBytecode(expression.getContext(), ctx, mv);

        // 处理结果（如果不是void）
        int resultIndex = -1;
        if (resultType != Type.VOID) {
            resultIndex = ctx.allocateLocalVar(Type.OBJECT);
            mv.visitVarInsn(ASTORE, resultIndex);
        }

        // 恢复原环境
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(ALOAD, oldEnvIndex);
        mv.visitFieldInsn(PUTFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());

        // 恢复结果到栈上（如果不是void）
        if (resultType != Type.VOID) {
            mv.visitVarInsn(ALOAD, resultIndex);
        }
        return resultType;
    }
}