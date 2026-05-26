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
            setPrimitiveArg(ctx, i, target, t, interpreter.resultPrimitive);
        } else if (expected.isPrimitive()) {
            setArgFromObject(ctx, i, expected, interpreter.resultRef);
        } else {
            ctx.setRef(i, TypeCompatibility.convertValue(interpreter.resultRef, expected.getSource()));
        }
    }

    private static void setPrimitiveArg(FunctionContext<?> ctx, int i, Type target, Type source, long raw) {
        if (target == Type.I) { ctx.setInt(i, readAsInt(source, raw)); }
        else if (target == Type.D) { ctx.setDouble(i, readAsDouble(source, raw)); }
        else if (target == Type.J) { ctx.setLong(i, readAsLong(source, raw)); }
        else if (target == Type.F) { ctx.setFloat(i, (float) readAsDouble(source, raw)); }
        else if (target == Type.Z) {
            if (source == Type.Z) { ctx.setBool(i, raw != 0L); }
            else { ctx.setInt(i, readAsInt(source, raw)); }
        }
    }

    private static void setArgFromObject(FunctionContext<?> ctx, int i, Type expected, Object ref) {
        if (ref instanceof Number) {
            Number num = (Number) ref;
            if (expected == Type.I) ctx.setInt(i, num.intValue());
            else if (expected == Type.Z) ctx.setInt(i, num.intValue());
            else if (expected == Type.J) ctx.setLong(i, num.longValue());
            else if (expected == Type.F) ctx.setFloat(i, num.floatValue());
            else if (expected == Type.D) ctx.setDouble(i, num.doubleValue());
        } else if (ref instanceof Boolean) {
            boolean value = (Boolean) ref;
            if (expected == Type.Z) ctx.setBool(i, value);
            else if (expected == Type.I) ctx.setInt(i, value ? 1 : 0);
            else if (expected == Type.J) ctx.setLong(i, value ? 1L : 0L);
            else if (expected == Type.F) ctx.setFloat(i, value ? 1F : 0F);
            else if (expected == Type.D) ctx.setDouble(i, value ? 1D : 0D);
        } else {
            throw new ClassCastException("Cannot convert " + (ref == null ? "null" : ref.getClass().getName()) + " to " + expected);
        }
    }

    private static int readAsInt(Type t, long raw) {
        if (t == Type.I || t == Type.Z || t == Type.J) return (int) raw;
        if (t == Type.F) return (int) Float.intBitsToFloat((int) raw);
        if (t == Type.D) return (int) Double.longBitsToDouble(raw);
        return 0;
    }

    private static long readAsLong(Type t, long raw) {
        if (t == Type.I || t == Type.Z) return (int) raw;
        if (t == Type.J) return raw;
        if (t == Type.F) return (long) Float.intBitsToFloat((int) raw);
        if (t == Type.D) return (long) Double.longBitsToDouble(raw);
        return 0;
    }

    private static double readAsDouble(Type t, long raw) {
        if (t == Type.D) return Double.longBitsToDouble(raw);
        if (t == Type.I || t == Type.Z) return (int) raw;
        if (t == Type.J) return raw;
        if (t == Type.F) return Float.intBitsToFloat((int) raw);
        return 0;
    }

    public static void emitFinishCall(Type returnType, boolean knownSync, MethodVisitor mv) {
        String ctxDesc = FunctionContext.TYPE.getDescriptor();
        String method, returnDesc;
        if (returnType == Type.I || returnType == Type.Z) {
            method = "finishCallInt"; returnDesc = "I";
        } else if (returnType == Type.J) {
            method = "finishCallLong"; returnDesc = "J";
        } else if (returnType == Type.D) {
            method = "finishCallDouble"; returnDesc = "D";
        } else if (returnType == Type.F) {
            method = "finishCallFloat"; returnDesc = "F";
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
                "(" + FunctionContextPool.TYPE + Environment.TYPE + Function.TYPE + "I)" + FunctionContext.TYPE,
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
            emitSetPrimitive(target, ctxPath, mv);
        } else if (expected != null && expected.isPrimitive()) {
            emitUnboxToPrimitive(expected, ctxPath, mv);
        } else {
            if (expected != null) {
                // Java 导出方法按强类型调用，写入参数槽前先完成脚本字面量到 enum 的转换。
                Instructions.emitValueConversion(mv, expected.getSource());
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(I" + Type.OBJECT + ")V", false);
        }
    }

    private static void emitSetPrimitive(Type t, String ctxPath, MethodVisitor mv) {
        String method, desc;
        if (t == Type.I) { method = "setInt"; desc = "(II)V"; }
        else if (t == Type.Z) { method = "setBool"; desc = "(IZ)V"; }
        else if (t == Type.J) { method = "setLong"; desc = "(IJ)V"; }
        else if (t == Type.F) { method = "setFloat"; desc = "(IF)V"; }
        else if (t == Type.D) { method = "setDouble"; desc = "(ID)V"; }
        else return;
        mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, method, desc, false);
    }

    private static void emitUnboxToPrimitive(Type expected, String ctxPath, MethodVisitor mv) {
        if (expected != Type.I && expected != Type.Z && expected != Type.J && expected != Type.F && expected != Type.D) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(I" + Type.OBJECT + ")V", false);
            return;
        }
        Instructions.emitUnbox(expected, mv);
        emitSetPrimitive(expected, ctxPath, mv);
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
