package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.TryExpression;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;
import static org.tabooproject.fluxon.runtime.Type.VOID;

public class TryEvaluator extends ExpressionEvaluator<TryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.TRY;
    }

    @Override
    public Object evaluate(Interpreter interpreter, TryExpression result) {
        Object value = null;
        try {
            value = interpreter.evaluate(result.getBody());
        } catch (Throwable ex) {
            if (result.getCatchName() != null) {
                interpreter.getEnvironment().assign(result.getCatchName(), ex, result.getPosition());
            }
            if (result.getCatchBody() != null) {
                value = interpreter.evaluate(result.getCatchBody());
            }
        }
        if (result.getFinallyBody() != null) {
            interpreter.evaluate(result.getFinallyBody());
        }
        return value;
    }

    @Override
    public Type generateBytecode(TryExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取各个块的评估器
        Evaluator<ParseResult> tryEval = null;
        if (result.getBody() != null) {
            tryEval = ctx.getEvaluator(result.getBody());
            if (tryEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for try body");
            }
        }
        Evaluator<ParseResult> catchEval = null;
        if (result.getCatchBody() != null) {
            catchEval = ctx.getEvaluator(result.getCatchBody());
            if (catchEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for catch body");
            }
        }
        Evaluator<ParseResult> finallyEval = null;
        if (result.getFinallyBody() != null) {
            finallyEval = ctx.getEvaluator(result.getFinallyBody());
            if (finallyEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for finally body");
            }
        }
        
        // 分配局部变量用于存储结果值
        int valueVar = ctx.allocateLocalVar(Type.OBJECT);
        // 预分配异常变量槽位 - 必须在 visitTryCatchBlock 之前分配
        // 避免栈映射表与实际局部变量表不一致
        int exVar = ctx.allocateLocalVar(Type.OBJECT);
        
        // 创建标签
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchStart = new Label();
        Label finallyLabel = new Label();

        // 初始化 value 为 null
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, valueVar);
        // 设置异常表 - 必须在代码生成前设置
        mv.visitTryCatchBlock(tryStart, tryEnd, catchStart, "java/lang/Throwable");
        // Try 块开始
        mv.visitLabel(tryStart);
        // 执行 try body
        if (tryEval != null) {
            Type tryType = tryEval.generateBytecode(result.getBody(), ctx, mv);
            if (tryType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitVarInsn(ASTORE, valueVar);
        }
        // Try 块结束
        mv.visitLabel(tryEnd);
        // 正常执行路径：跳到 finally 块
        mv.visitJumpInsn(GOTO, finallyLabel);
        // Catch 块开始
        mv.visitLabel(catchStart);
        // 异常已经在栈顶，需要存储到预分配的槽位
        mv.visitVarInsn(ASTORE, exVar);
        // 如果有 catch 变量名，则将异常赋值给该变量
        if (result.getCatchName() != null) {
            // 加载 env
            BytecodeUtils.loadEnvironment(mv, ctx);
            // 加载变量名
            mv.visitLdcInsn(result.getCatchName());
            // 加载异常对象
            mv.visitVarInsn(ALOAD, exVar);
            // 加载 position
            mv.visitLdcInsn(result.getPosition());
            // 调用 assign 方法
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "assign", "(" + STRING + OBJECT + I + ")" + VOID, false);
        }
        // 执行 catch body
        if (catchEval != null) {
            Type catchType = catchEval.generateBytecode(result.getCatchBody(), ctx, mv);
            if (catchType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
                mv.visitVarInsn(ASTORE, valueVar);
            } else {
                // 使用临时变量存储 catch body 的结果，避免直接赋值导致的字节码验证问题
                // 这种"冗余"操作确保了控制流合并点的类型信息清晰，使反编译器能正确处理
                int catchVar = ctx.allocateLocalVar(Type.OBJECT);
                mv.visitVarInsn(ASTORE, catchVar);
                mv.visitVarInsn(ALOAD, catchVar);
                mv.visitVarInsn(ASTORE, valueVar);
            }
        }
        // Finally 块
        mv.visitLabel(finallyLabel);
        if (finallyEval != null) {
            Type finallyType = finallyEval.generateBytecode(result.getFinallyBody(), ctx, mv);
            if (finallyType != Type.VOID) {
                // 丢弃 finally 的返回值
                mv.visitInsn(POP);
            }
        }
        // 返回 value
        mv.visitVarInsn(ALOAD, valueVar);
        return Type.OBJECT;
    }
}
