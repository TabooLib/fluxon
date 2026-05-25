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
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

/**
 * 函数调用处理器工具类
 *
 * @author sky
 */
public final class FunctionCallHandlers {

    // 类型转换指令矩阵: [from][to], -1 表示无需转换
    // 索引: I/Z=0, J=1, F=2, D=3
    private static final int[][] CONV_MATRIX = {
            {-1, I2L, I2F, I2D},
            {L2I, -1, L2F, L2D},
            {F2I, F2L, -1, F2D},
            {D2I, D2L, D2F, -1}
    };

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
            ctx.setRef(i, interpreter.resultRef);
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
            emitPrimitiveConversion(t, target, mv);
            emitSetPrimitive(target, ctxPath, mv);
        } else if (expected != null && expected.isPrimitive()) {
            emitUnboxToPrimitive(expected, ctxPath, mv);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(I" + Type.OBJECT + ")V", false);
        }
    }

    /**
     * 生成原始类型之间的转换指令（I2D、L2D 等）
     * 栈顶值从 from 类型转换为 to 类型
     */
    public static void emitPrimitiveConversion(Type from, Type to, MethodVisitor mv) {
        int fi = typeIndex(from), ti = typeIndex(to);
        if (fi < 0 || ti < 0) return;
        int opcode = CONV_MATRIX[fi][ti];
        if (opcode >= 0) mv.visitInsn(opcode);
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
        String valueMethod, setMethod, setDesc, retDesc;
        if (expected == Type.Z) {
            Instructions.emitUnboxBooleanCompatible(mv);
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setBool", "(IZ)V", false);
            return;
        }
        mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
        if (expected == Type.I) {
            valueMethod = "intValue"; setMethod = "setInt"; setDesc = "(II)V"; retDesc = "I";
        } else if (expected == Type.J) {
            valueMethod = "longValue"; setMethod = "setLong"; setDesc = "(IJ)V"; retDesc = "J";
        } else if (expected == Type.F) {
            valueMethod = "floatValue"; setMethod = "setFloat"; setDesc = "(IF)V"; retDesc = "F";
        } else if (expected == Type.D) {
            valueMethod = "doubleValue"; setMethod = "setDouble"; setDesc = "(ID)V"; retDesc = "D";
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(I" + Type.OBJECT + ")V", false);
            return;
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", valueMethod, "()" + retDesc, false);
        mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, setMethod, setDesc, false);
    }

    /**
     * 生成 Object → 原始类型的拆箱指令（栈操作，不涉及 FunctionContext）
     * 栈顶 Object 转换为目标原始类型
     */
    public static void emitUnbox(Type target, MethodVisitor mv) {
        if (target == Type.Z) {
            Instructions.emitUnboxBooleanCompatible(mv);
            return;
        }
        mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
        if (target == Type.I) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
        } else if (target == Type.J) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
        } else if (target == Type.D) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
        } else if (target == Type.F) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
        }
    }

    /**
     * 生成原始类型 → Object 的装箱指令（栈操作）
     */
    public static void emitBox(Type source, MethodVisitor mv) {
        if (source == Type.Z) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else if (source == Type.I) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (source == Type.J) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (source == Type.D) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (source == Type.F) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        }
    }

    private static int typeIndex(Type t) {
        if (t == Type.I || t == Type.Z) return 0;
        if (t == Type.J) return 1;
        if (t == Type.F) return 2;
        if (t == Type.D) return 3;
        return -1;
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
