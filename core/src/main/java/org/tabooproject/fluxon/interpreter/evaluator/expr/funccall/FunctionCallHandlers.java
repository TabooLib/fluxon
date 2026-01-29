package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;
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
        try {
            ctx.setInterpreter(interpreter);
            function.call(ctx);
            Type returnType = ctx.getReturnType();
            if (returnType != null && returnType != Type.VOID && returnType.isPrimitive()) {
                interpreter.resultPrimitive = ctx.getReturnPrimitive();
                ctx.close();
                return returnType;
            } else {
                interpreter.resultRef = ctx.getReturnRef();
                ctx.close();
                return Type.OBJECT;
            }
        } catch (Throwable ex) {
            ctx.close();
            throw ex;
        }
    }

    public static void setArgument(FunctionContext<?> ctx, int i, Type t, Type expected, Interpreter interpreter) {
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
        switch (target.getDescriptor()) {
            case "I": case "Z": ctx.setInt(i, readAsInt(source, raw)); break;
            case "J": ctx.setLong(i, readAsLong(source, raw)); break;
            case "F": ctx.setFloat(i, (float) readAsDouble(source, raw)); break;
            case "D": ctx.setDouble(i, readAsDouble(source, raw)); break;
        }
    }

    private static void setArgFromObject(FunctionContext<?> ctx, int i, Type expected, Object ref) {
        if (ref instanceof Number) {
            Number num = (Number) ref;
            if (expected == Type.I || expected == Type.Z) ctx.setInt(i, num.intValue());
            else if (expected == Type.J) ctx.setLong(i, num.longValue());
            else if (expected == Type.F) ctx.setFloat(i, num.floatValue());
            else if (expected == Type.D) ctx.setDouble(i, num.doubleValue());
        } else if (ref instanceof Boolean) {
            ctx.setInt(i, (Boolean) ref ? 1 : 0);
        } else {
            throw new ClassCastException("Cannot convert " + (ref == null ? "null" : ref.getClass().getName()) + " to " + expected);
        }
    }

    private static int readAsInt(Type t, long raw) {
        switch (t.getDescriptor()) {
            case "I": case "Z": case "J": return (int) raw;
            case "F": return (int) Float.intBitsToFloat((int) raw);
            case "D": return (int) Double.longBitsToDouble(raw);
            default: return 0;
        }
    }

    private static long readAsLong(Type t, long raw) {
        switch (t.getDescriptor()) {
            case "I": case "Z": return (int) raw;
            case "J": return raw;
            case "F": return (long) Float.intBitsToFloat((int) raw);
            case "D": return (long) Double.longBitsToDouble(raw);
            default: return 0;
        }
    }

    private static double readAsDouble(Type t, long raw) {
        switch (t.getDescriptor()) {
            case "I": case "Z": return (int) raw;
            case "J": return raw;
            case "F": return Float.intBitsToFloat((int) raw);
            case "D": return Double.longBitsToDouble(raw);
            default: return 0;
        }
    }

    public static void emitFinishCall(Type returnType, MethodVisitor mv) {
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
            method = "finishCall"; returnDesc = Type.OBJECT.getDescriptor();
        }
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), method, "(" + ctxDesc + ")" + returnDesc, false);
    }

    public static void emitSetArg(Type t, Type expected, MethodVisitor mv) {
        String ctxPath = FunctionContext.TYPE.getPath();
        if (t.isPrimitive()) {
            Type target = (expected != null && expected.isPrimitive()) ? expected : t;
            emitTypeConversion(t, target, mv);
            emitSetPrimitive(target, ctxPath, mv);
        } else if (expected != null && expected.isPrimitive()) {
            emitUnboxToPrimitive(expected, ctxPath, mv);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(I" + Type.OBJECT + ")V", false);
        }
    }

    private static void emitTypeConversion(Type from, Type to, MethodVisitor mv) {
        int fi = typeIndex(from), ti = typeIndex(to);
        if (fi < 0 || ti < 0) return;
        int opcode = CONV_MATRIX[fi][ti];
        if (opcode >= 0) mv.visitInsn(opcode);
    }

    private static void emitSetPrimitive(Type t, String ctxPath, MethodVisitor mv) {
        String method, desc;
        if (t == Type.I || t == Type.Z) { method = "setInt"; desc = "(II)V"; }
        else if (t == Type.J) { method = "setLong"; desc = "(IJ)V"; }
        else if (t == Type.F) { method = "setFloat"; desc = "(IF)V"; }
        else if (t == Type.D) { method = "setDouble"; desc = "(ID)V"; }
        else return;
        mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, method, desc, false);
    }

    private static void emitUnboxToPrimitive(Type expected, String ctxPath, MethodVisitor mv) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
        String valueMethod, setMethod, setDesc, retDesc;
        if (expected == Type.I || expected == Type.Z) {
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

    private static int typeIndex(Type t) {
        if (t == Type.I || t == Type.Z) return 0;
        if (t == Type.J) return 1;
        if (t == Type.F) return 2;
        if (t == Type.D) return 3;
        return -1;
    }
}
