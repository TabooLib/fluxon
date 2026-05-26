package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

/**
 * 函数调用处理器工具类
 *
 * @author sky
 */
public final class FunctionCallHandlers {

    private FunctionCallHandlers() {}

    public static Type executeSync(Interpreter interpreter, FunctionContext<?> ctx) {
        return executeSync(interpreter, ctx, ctx.getFunction());
    }

    public static Type executeSync(Interpreter interpreter, FunctionContext<?> ctx, Function function) {
        if (function.isAsync() || function.isPrimarySync()) {
            interpreter.resultRef = Intrinsics.finishCall(ctx, interpreter);
            return Type.OBJECT;
        }
        return executeKnownSync(interpreter, ctx, function);
    }

    /**
     * 已缓存解析结果保证同步，热路径跳过 async/primarySync 分支。
     */
    public static Type executeKnownSync(Interpreter interpreter, FunctionContext<?> ctx, Function function) {
        FunctionContextPool pool = ctx.getPool();
        try {
            ctx.setInterpreter(interpreter);
            function.call(ctx);
            Type returnType = ctx.getReturnType();
            if (returnType != null && returnType != Type.VOID && returnType.isPrimitive()) {
                interpreter.resultPrimitive = ctx.getReturnPrimitive();
                pool.releaseTop();
                return returnType;
            } else {
                interpreter.resultRef = ctx.getReturnRef();
                pool.releaseTop();
                return Type.OBJECT;
            }
        } catch (Throwable ex) {
            pool.releaseTop();
            throw ex;
        }
    }

    public static void setArgument(Interpreter interpreter, FunctionContext<?> ctx, int i, Type t, Type expected) {
        if (t.isPrimitive()) {
            Type target = (expected.isPrimitive() && !t.equals(expected)) ? expected : t;
            ctx.setArgumentFromBits(i, target, t, interpreter.resultPrimitive);
        } else if (expected.isPrimitive()) {
            ctx.setArgumentFromObject(i, expected, interpreter.resultRef);
        } else {
            ctx.setRef(i, TypeCompatibility.convertValue(interpreter.resultRef, expected.getSource()));
        }
    }

    public static void emitFinishCall(Type returnType, boolean knownSync, MethodVisitor mv) {
        String ctxDesc = FunctionContext.TYPE.getDescriptor();
        String method, returnDesc;
        if (returnType == Type.I || returnType == Type.Z) {
            method = "finishCallInt"; returnDesc = Type.I.getDescriptor();
        } else if (returnType == Type.J) {
            method = "finishCallLong"; returnDesc = Type.J.getDescriptor();
        } else if (returnType == Type.D) {
            method = "finishCallDouble"; returnDesc = Type.D.getDescriptor();
        } else if (returnType == Type.F) {
            method = "finishCallFloat"; returnDesc = Type.F.getDescriptor();
        } else {
            method = knownSync ? "finishCallSync" : "finishCall";
            returnDesc = Type.OBJECT.getDescriptor();
        }
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), method, "(" + ctxDesc + ")" + returnDesc, false);
    }

    /**
     * 生成 prepareCallDirect 调用的字节码
     * 栈顶需已加载函数引用（Function 类型），此方法负责其余部分：
     * loadPool, loadEnv, [function already on stack], argCount → prepareCallDirect → astore
     *
     * @param functionLoader 将函数引用压栈的回调，在 pool 和 env 之后执行
     * @return PrepareCallResult 包含 ctxSlot
     */
    public static FunctionCallHandler.PrepareCallResult emitPrepareCallDirect(
            CodeContext ctx,
            MethodVisitor mv,
            int argCount,
            Runnable functionLoader
    ) {
        Instructions.loadPool(mv, ctx);
        Instructions.loadEnvironment(mv, ctx);
        functionLoader.run();
        mv.visitLdcInsn(argCount);
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "prepareCallDirect",
                "(" + FunctionContextPool.TYPE + Environment.TYPE + Function.TYPE + Type.I + ")" + FunctionContext.TYPE,
                false
        );
        int ctxSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, ctxSlot);
        return new FunctionCallHandler.PrepareCallResult(ctxSlot);
    }

    public static void emitSetArg(Type t, Type expected, MethodVisitor mv) {
        String ctxPath = FunctionContext.TYPE.getPath();
        if (t.isPrimitive()) {
            Type target = (expected != null && expected.isPrimitive()) ? expected : t;
            Instructions.emitPrimitiveConversion(t, target, mv);
            Instructions.emitFunctionContextSetArgument(mv, target);
        } else if (expected != null && expected.isPrimitive()) {
            Instructions.emitUnbox(mv, expected);
            Instructions.emitFunctionContextSetArgument(mv, expected);
        } else {
            if (expected != null) {
                // Java 导出方法按强类型调用，写入参数槽前先完成脚本字面量到 enum 的转换。
                Instructions.emitValueConversion(mv, expected.getSource());
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(" + Type.I + Type.OBJECT + ")" + Type.VOID, false);
        }
    }

    /**
     * 生成参数表达式字节码，返回栈上类型
     */
    public static Type emitArgExpression(ParseResult arg, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> argEval = ctx.getEvaluator(arg);
        if (argEval == null) throw new EvaluatorNotFoundError("No evaluator found for argument expression");
        Type t = argEval.generateBytecode(arg, ctx, mv);
        if (t == Type.VOID) throw new VoidError("Void type is not allowed for function arguments");
        return t;
    }
}
