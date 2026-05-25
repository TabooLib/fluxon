package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * DirectBinding 字节码生成器
 * 当函数在编译期可完全确定时，直接生成 INVOKESTATIC 调用 Java 方法，
 * 跳过 prepareCall / FunctionContext / finishCall 整个调用框架。
 *
 * @author sky
 */
public final class DirectBindingEmitter {

    private DirectBindingEmitter() {}

    /**
     * 尝试为函数调用生成 DirectBinding 字节码
     *
     * @return 返回类型，null 表示不适用（调用方需 fallback 到框架路径）
     */
    public static Type tryEmit(FunctionCallExpression expr, ParseResult[] args, Type[] argTypes, CodeContext ctx, MethodVisitor mv) {
        // 用户定义函数不走 DirectBinding
        if (ctx.getUserFunctionOwner(expr.getFunctionName()) != null) return null;
        // 已解析的扩展函数
        Function resolvedExt = expr.getResolvedExtensionFunction();
        if (resolvedExt != null && expr.getResolvedTargetClass() != null) {
            return tryEmitExtension(resolvedExt, expr.getResolvedTargetClass(), args, ctx, mv);
        }
        // 有 extensionPosition 但未解析到具体函数，不能 DirectBinding
        if (expr.getExtensionPosition() != null) return null;
        Type throwResult = tryEmitThrow(expr, args, ctx, mv);
        if (throwResult != null) return throwResult;
        Type outputResult = tryEmitOutput(expr, args, ctx, mv);
        if (outputResult != null) return outputResult;
        // 系统函数
        OverloadSet overloadSet = FluxonRuntime.getInstance().getSystemFunctions().get(expr.getFunctionName());
        if (overloadSet == null) return null;
        // 多重载 + 存在未知类型参数时不能在编译期确定重载
        if (overloadSet.size() > 1 && hasUnknownType(argTypes)) return null;
        Function function = overloadSet.resolve(argTypes != null ? argTypes : new Type[args.length]);
        if (function == null) return null;
        DirectBinding binding = function.getDirectBinding();
        if (binding == null || !canDirectBind(function)) return null;
        return emitInvoke(function, binding, args, ctx, mv, 0);
    }

    private static Type tryEmitThrow(FunctionCallExpression expr, ParseResult[] args, CodeContext ctx, MethodVisitor mv) {
        if (!"throw".equals(expr.getFunctionName()) || args.length != 1) {
            return null;
        }
        Type argType = FunctionCallHandlers.emitArgExpression(args[0], ctx, mv);
        if (argType.isPrimitive()) {
            FunctionCallHandlers.emitBox(argType, mv);
        }
        int valueSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, valueSlot);
        Label runtimeException = new Label();
        mv.visitVarInsn(ALOAD, valueSlot);
        mv.visitTypeInsn(INSTANCEOF, "java/lang/Error");
        mv.visitJumpInsn(IFEQ, runtimeException);
        mv.visitVarInsn(ALOAD, valueSlot);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Error");
        mv.visitInsn(ATHROW);
        mv.visitLabel(runtimeException);
        mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, valueSlot);
        mv.visitMethodInsn(INVOKEVIRTUAL, Type.OBJECT.getPath(), "toString", "()" + Type.STRING, false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(" + Type.STRING + ")V", false);
        mv.visitInsn(ATHROW);
        return Type.VOID;
    }

    private static Type tryEmitOutput(FunctionCallExpression expr, ParseResult[] args, CodeContext ctx, MethodVisitor mv) {
        String functionName = expr.getFunctionName();
        if (!"print".equals(functionName) && !"error".equals(functionName)) {
            return null;
        }
        if (args.length > 1) {
            return null;
        }
        // print/error 只依赖当前 Environment 的输出流，直接发出 PrintStream 调用可跳过 FunctionContext。
        Instructions.loadEnvironment(mv, ctx);
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "print".equals(functionName) ? "getOut" : "getErr", "()Ljava/io/PrintStream;", false);
        if (args.length == 0) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "()V", false);
            return Type.VOID;
        }
        Type argType = FunctionCallHandlers.emitArgExpression(args[0], ctx, mv);
        String descriptor = getPrintlnDescriptor(argType);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", descriptor, false);
        return Type.VOID;
    }

    private static String getPrintlnDescriptor(Type type) {
        if (type == Type.I) return "(" + Type.I + ")V";
        if (type == Type.J) return "(" + Type.J + ")V";
        if (type == Type.F) return "(" + Type.F + ")V";
        if (type == Type.D) return "(" + Type.D + ")V";
        if (type == Type.Z) return "(" + Type.Z + ")V";
        if (type == Type.STRING) return "(" + Type.STRING + ")V";
        return "(" + Type.OBJECT + ")V";
    }

    /**
     * 扩展函数 DirectBinding
     * 生成序列：loadEnvironment → getTarget → CHECKCAST → 参数求值 → INVOKESTATIC
     */
    private static Type tryEmitExtension(Function function, Class<?> targetClass, ParseResult[] args, CodeContext ctx, MethodVisitor mv) {
        DirectBinding binding = function.getDirectBinding();
        if (binding == null || !canDirectBind(function)) return null;
        // 加载 target：environment.getTarget() + CHECKCAST
        Instructions.loadEnvironment(mv, ctx);
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getTarget", "()" + Type.OBJECT, false);
        mv.visitTypeInsn(CHECKCAST, targetClass.getName().replace('.', '/'));
        // descriptor 第一个参数是 target，用户参数从第二个开始
        return emitInvoke(function, binding, args, ctx, mv, 1);
    }

    /**
     * 生成参数求值 + INVOKESTATIC + 返回类型修正
     *
     * @param skipDescriptorParams 跳过 descriptor 的前 N 个参数（扩展函数跳过 target）
     */
    private static Type emitInvoke(Function function, DirectBinding binding, ParseResult[] args, CodeContext ctx, MethodVisitor mv, int skipDescriptorParams) {
        FunctionSignature signature = function.getSignature();
        Type[] jvmParamTypes = binding.reconcileParamTypes(signature.getParameterTypes(), skipDescriptorParams);
        // descriptor 中期望具体引用类型的参数位置（如 String），栈上为 Object 时需要 CHECKCAST
        String[] castTargets = binding.getDescriptorParamCastTargets(skipDescriptorParams, args.length);
        for (int i = 0; i < args.length; i++) {
            Type actual = FunctionCallHandlers.emitArgExpression(args[i], ctx, mv);
            Type expected = i < jvmParamTypes.length ? jvmParamTypes[i] : Type.OBJECT;
            emitArgConversion(actual, expected, mv);
            // 栈上是 Object 但 descriptor 期望具体引用子类 → CHECKCAST
            if (castTargets != null && castTargets[i] != null && !actual.isPrimitive()) {
                mv.visitTypeInsn(CHECKCAST, castTargets[i]);
            }
        }
        mv.visitMethodInsn(INVOKESTATIC, binding.getOwner(), binding.getMethod(), binding.getDescriptor(), false);
        return binding.reconcileReturnType(signature.getReturnType());
    }

    private static boolean canDirectBind(Function function) {
        return function.getSignature() != null && !function.isAsync() && !function.isPrimarySync();
    }

    /**
     * 参数类型转换：actual → expected
     */
    private static void emitArgConversion(Type actual, Type expected, MethodVisitor mv) {
        if (actual == expected) return;
        if (actual.isPrimitive() && expected.isPrimitive()) {
            FunctionCallHandlers.emitPrimitiveConversion(actual, expected, mv);
        } else if (!actual.isPrimitive() && expected.isPrimitive()) {
            FunctionCallHandlers.emitUnbox(expected, mv);
        } else if (actual.isPrimitive()) {
            FunctionCallHandlers.emitBox(actual, mv);
        }
    }

    private static boolean hasUnknownType(Type[] types) {
        if (types == null) return false;
        for (Type t : types) {
            if (t == Type.OBJECT) return true;
        }
        return false;
    }
}
