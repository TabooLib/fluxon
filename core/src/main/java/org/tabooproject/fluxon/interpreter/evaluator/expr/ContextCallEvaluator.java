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
        ContextEnvironment contextEnv = new ContextEnvironment(interpreter.getEnvironment(), target, expression.getLocalVariables(), "context-call");
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

        // 计算目标值
        Type targetType = targetEval.generateBytecode(expression.getTarget(), ctx, mv);
        if (targetType == Type.VOID) {
            throw new VoidValueException("Void type is not allowed for context call target");
        }

        // 调用 enterContextScope 方法
        mv.visitVarInsn(ALOAD, 0);                       // this
        mv.visitInsn(SWAP);                              // 将目标值移到 this 下面
        mv.visitLdcInsn(expression.getLocalVariables()); // localVariables 参数
        mv.visitLdcInsn("context-call");
        mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "enterContextScope", "(" + Type.OBJECT + Type.I + Type.STRING + ")V", false);

        // 在新环境中求值上下文表达式
        Type resultType = contextEval.generateBytecode(expression.getContext(), ctx, mv);

        // 处理结果（如果不是 void）
        int resultIndex = -1;
        if (resultType != Type.VOID) {
            resultIndex = ctx.allocateLocalVar(Type.OBJECT);
            mv.visitVarInsn(ASTORE, resultIndex);
        }

        // 调用 exitScope 方法恢复原环境
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "exitScope", "()V", false);

        // 恢复结果到栈上（如果不是 void）
        if (resultType != Type.VOID) {
            mv.visitVarInsn(ALOAD, resultIndex);
        }
        return resultType;
    }
}