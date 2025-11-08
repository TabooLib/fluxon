package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.TryExpression;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

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
        
        // 分配局部变量用于存储结果值 (对应字节码中的 astore_3)
        int valueVar = ctx.allocateLocalVar(Type.OBJECT);
        
        // 创建标签
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchStart = new Label();
        Label finallyLabel = new Label();

        // 初始化 value 为 null (对应字节码 0: aconst_null, 1: astore_3)
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, valueVar);
        
        // 设置异常表 - 必须在代码生成前设置
        mv.visitTryCatchBlock(tryStart, tryEnd, catchStart, "java/lang/Throwable");
        
        // Try 块开始
        mv.visitLabel(tryStart);
        
        // 执行 try body (对应字节码 2-10)
        if (tryEval != null) {
            Type tryType = tryEval.generateBytecode(result.getBody(), ctx, mv);
            if (tryType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitVarInsn(ASTORE, valueVar);
        }
        
        // Try 块结束
        mv.visitLabel(tryEnd);
        
        // 正常执行路径：跳到 finally 块 (对应字节码 11: goto 56)
        mv.visitJumpInsn(GOTO, finallyLabel);
        
        // Catch 块开始 (对应字节码 14)
        mv.visitLabel(catchStart);

        // 异常已经在栈顶，需要存储它 (对应字节码 14: astore ex)
        int exVar = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, exVar);

        // 如果有 catch 变量名，则将异常赋值给该变量 (对应字节码 16-37)
        if (result.getCatchName() != null) {
            // 加载 this (对应字节码 23: aload_1 但这里是 aload_0)
            mv.visitVarInsn(ALOAD, 0);
            // 加载变量名 (对应字节码 28: invokevirtual getCatchName)
            mv.visitLdcInsn(result.getCatchName());
            // 加载异常对象 (对应字节码 31: aload ex)
            mv.visitVarInsn(ALOAD, exVar);
            // 加载 position (对应字节码 34: invokevirtual getPosition -> 37: assign)
            mv.visitLdcInsn(result.getPosition());
            // 调用 assign 方法
            mv.visitMethodInsn(INVOKEVIRTUAL,
                ctx.getClassName().replace('.', '/'),
                "assign",
                "(Ljava/lang/String;Ljava/lang/Object;I)V",
                false);
        }
        
        // 执行 catch body (对应字节码 40-55)
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
        
        // Finally 块 (对应字节码 56-71)
        mv.visitLabel(finallyLabel);
        if (finallyEval != null) {
            Type finallyType = finallyEval.generateBytecode(result.getFinallyBody(), ctx, mv);
            if (finallyType != Type.VOID) {
                // 丢弃 finally 的返回值 (对应字节码 71: pop)
                mv.visitInsn(POP);
            }
        }
        
        // 返回 value (对应字节码 72-73)
        mv.visitVarInsn(ALOAD, valueVar);
        return Type.OBJECT;
    }
}
