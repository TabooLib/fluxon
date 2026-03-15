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
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.TryExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;

public class TryEvaluator extends ExpressionEvaluator<TryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.TRY;
    }

    @Override
    public Type evaluate(Interpreter interpreter, TryExpression result) {
        Type valueType = Type.OBJECT;
        try {
            valueType = interpreter.evaluate(result.getBody());
        } catch (Throwable ex) {
            // return 信号不应被 catch 拦截，直接跳过 catch 块
            if (interpreter.hasReturn) {
                // hasReturn 已设，不执行 catch，进入 finally
            } else {
                if (result.getCatchName() != null) {
                    int position = result.getPosition();
                    if (position >= 0) {
                        // Env-free 路径：写入 FunctionContext
                        FunctionContext<?> ctx = interpreter.activeFunctionContext;
                        if (ctx != null) {
                            ctx.setLocal(position, ex);
                        } else {
                            interpreter.getEnvironment().setLocalRef(position, ex);
                        }
                    } else {
                        interpreter.getEnvironment().setRootVariable(result.getCatchName(), ex);
                    }
                }
                if (result.getCatchBody() != null) {
                    valueType = interpreter.evaluate(result.getCatchBody());
                } else {
                    interpreter.resultRef = null;
                }
            }
        }
        if (result.getFinallyBody() != null) {
            // finally body 不影响 try/catch 的结果，保存/恢复 single fields
            long savedPrim = interpreter.resultPrimitive;
            Object savedRef = interpreter.resultRef;
            boolean savedHasReturn = interpreter.hasReturn;
            Object savedReturnValue = interpreter.returnValue;
            interpreter.hasReturn = false;
            interpreter.evaluate(result.getFinallyBody());
            interpreter.resultPrimitive = savedPrim;
            interpreter.resultRef = savedRef;
            interpreter.hasReturn = savedHasReturn;
            interpreter.returnValue = savedReturnValue;
        }
        return valueType;
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
        int saved = ctx.getLocalVarIndex();
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
            } else {
                boxing(tryType, mv);
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
            int position = result.getPosition();
            if (position >= 0 && ctx.isEnvFreeMode()) {
                // Env-free 模式：直接存入 JVM 局部变量
                int jvmSlot = ctx.getJvmSlot(position);
                mv.visitVarInsn(ALOAD, exVar);
                mv.visitVarInsn(ASTORE, jvmSlot);
            } else {
                Instructions.loadEnvironment(mv, ctx);
                if (position >= 0) {
                    mv.visitLdcInsn(position);
                    mv.visitVarInsn(ALOAD, exVar);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", "(" + I + OBJECT + ")" + VOID, false);
                } else {
                    mv.visitLdcInsn(result.getCatchName());
                    mv.visitVarInsn(ALOAD, exVar);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setRootVariable", "(" + STRING + OBJECT + ")" + VOID, false);
                }
            }
        }
        // 执行 catch body
        if (catchEval != null) {
            Type catchType = catchEval.generateBytecode(result.getCatchBody(), ctx, mv);
            if (catchType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
                mv.visitVarInsn(ASTORE, valueVar);
            } else {
                boxing(catchType, mv);
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
                mv.visitInsn((finallyType == Type.J || finallyType == Type.D) ? POP2 : POP);
            }
        }
        // 返回 value
        mv.visitVarInsn(ALOAD, valueVar);
        ctx.restoreLocalVarIndex(saved);
        return Type.OBJECT;
    }

    @Override
    public void analyzeTypes(TryExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getBody());
        analyzer.analyzeNode(result.getCatchBody());
        analyzer.analyzeNode(result.getFinallyBody());
    }
}
