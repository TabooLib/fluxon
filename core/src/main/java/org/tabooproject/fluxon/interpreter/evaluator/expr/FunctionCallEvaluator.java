package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionContextPool;
import org.tabooproject.fluxon.runtime.OverloadSet;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

public class FunctionCallEvaluator extends ExpressionEvaluator<FunctionCallExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public Type evaluate(Interpreter interpreter, FunctionCallExpression result) {
        ParseResult[] expressionArguments = result.getArguments();
        int argumentCount = expressionArguments.length;
        // 优先使用预解析的扩展函数（跳过动态解析）
        Function resolvedExt = result.getResolvedExtensionFunction();
        // 懒加载：解释模式下首次执行时解析扩展函数
        if (resolvedExt == null && result.getExtensionPosition() != null) {
            Object target = interpreter.getEnvironment().getTarget();
            if (target != null) {
                resolvedExt = result.resolveExtensionFunction(target.getClass(), new Type[argumentCount]);
            }
        }
        FunctionContextPool pool = interpreter.getPool();
        FunctionContext<?> ctx;
        if (resolvedExt != null) {
            ctx = Intrinsics.prepareCallDirect(pool, interpreter.getEnvironment(), resolvedExt, argumentCount);
        } else {
            ctx = Intrinsics.prepareCall(
                    pool,
                    interpreter.getEnvironment(),
                    result.getFunctionName(),
                    argumentCount,
                    result.getPositionIndex(),
                    result.getExtensionPositionIndex()
            );
        }
        // 获取函数期望的参数类型
        Function function = ctx.getFunction();
        Type[] expectedTypes = function.getSignature() != null ? function.getSignature().getParameterTypes() : null;
        for (int i = 0; i < argumentCount; i++) {
            Type t = interpreter.evaluate(expressionArguments[i]);
            Type expected = (expectedTypes != null && i < expectedTypes.length) ? expectedTypes[i] : Type.OBJECT;
            if (t.isPrimitive()) {
                Type target = (expected.isPrimitive() && !t.equals(expected)) ? expected : t;
                setPrimitiveArg(ctx, i, target, t, interpreter.resultPrimitive);
            } else if (expected.isPrimitive()) {
                setArgFromObject(ctx, i, expected, interpreter.resultRef);
            } else {
                ctx.setRef(i, interpreter.resultRef);
            }
        }
        // 处理 async / primarySync
        if (function.isAsync() || function.isPrimarySync()) {
            interpreter.resultRef = Intrinsics.finishCall(ctx, interpreter);
            return Type.OBJECT;
        }
        // 同步调用：直接执行并从 ctx 读取返回值
        try {
            ctx.setInterpreter(interpreter);
            function.call(ctx);
            Type returnType = ctx.getReturnType();
            // VOID 和非原始类型都走引用路径
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

    @Override
    public Type generateBytecode(FunctionCallExpression result, CodeContext ctx, MethodVisitor mv) {
        ParseResult[] arguments = result.getArguments();
        int argumentCount = arguments.length;
        int savedLocalVar = ctx.getLocalVarIndex();
        // 获取函数签名的期望参数类型
        Type[] expectedTypes = null;
        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        if (analyzer != null) {
            Type[] argTypes = new Type[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                argTypes[i] = analyzer.inferType(arguments[i]);
            }
            expectedTypes = result.resolveExpectedParameterTypes(argTypes);
        }
        // 1. 调用 prepareCall / prepareCallDirect → FunctionContext
        Function resolvedExt = result.getResolvedExtensionFunction();
        if (resolvedExt != null && result.getResolvedTargetClass() != null) {
            // 编译期已解析扩展函数，使用 prepareCallDirect
            int funcSlot = ctx.addResolvedExtensionFunction(result.getResolvedDispatchTableIndex(), result.getResolvedTargetClass(), result.getResolvedOverloadIndex());
            Instructions.loadPool(mv, ctx);
            Instructions.loadEnvironment(mv, ctx);
            mv.visitFieldInsn(GETSTATIC, ctx.getInternalName(), "RESOLVED_EXT_FUNCTIONS", "[" + Function.TYPE.getDescriptor());
            mv.visitLdcInsn(funcSlot);
            mv.visitInsn(AALOAD);
            mv.visitLdcInsn(argumentCount);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    Intrinsics.TYPE.getPath(),
                    "prepareCallDirect",
                    "(" + FunctionContextPool.TYPE + Environment.TYPE + Function.TYPE + "I)" + FunctionContext.TYPE,
                    false
            );
        } else {
            // 动态解析路径
            Instructions.loadPool(mv, ctx);
            Instructions.loadEnvironment(mv, ctx);
            mv.visitLdcInsn(result.getFunctionName());
            mv.visitLdcInsn(argumentCount);
            mv.visitLdcInsn(result.getPositionIndex());
            mv.visitLdcInsn(result.getExtensionPositionIndex());
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    Intrinsics.TYPE.getPath(),
                    "prepareCall",
                    "(" + FunctionContextPool.TYPE + Environment.TYPE + Type.STRING + "III)" + FunctionContext.TYPE,
                    false
            );
        }
        // 2. 存入局部变量
        int ctxSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, ctxSlot);
        // 3. 逐个设置参数
        for (int i = 0; i < argumentCount; i++) {
            mv.visitVarInsn(ALOAD, ctxSlot);
            mv.visitLdcInsn(i);
            Evaluator<ParseResult> argEval = ctx.getEvaluator(arguments[i]);
            if (argEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for argument expression");
            }
            Type t = argEval.generateBytecode(arguments[i], ctx, mv);
            if (t == Type.VOID) {
                throw new VoidError("Void type is not allowed for function arguments");
            }
            Type expected = (expectedTypes != null && i < expectedTypes.length) ? expectedTypes[i] : null;
            emitSetArg(t, expected, mv);
        }
        // 推断返回类型
        Type returnType = Type.OBJECT;
        if (analyzer != null) {
            Type inferred = inferResultType(result, analyzer);
            // VOID 按 OBJECT 处理（finishCall 返回 null）
            if (inferred != Type.VOID) {
                returnType = inferred;
            }
        }
        // 4. 调用 finishCall 并根据返回类型读取结果
        mv.visitVarInsn(ALOAD, ctxSlot);
        emitFinishCall(returnType, mv);
        // 释放临时变量槽位
        ctx.restoreLocalVarIndex(savedLocalVar);
        return returnType;
    }

    /**
     * 根据返回类型生成对应的 finishCall 调用
     */
    private static void emitFinishCall(Type returnType, MethodVisitor mv) {
        String ctxDesc = FunctionContext.TYPE.getDescriptor();
        if (returnType == Type.I || returnType == Type.Z) {
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "finishCallInt", "(" + ctxDesc + ")I", false);
        } else if (returnType == Type.J) {
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "finishCallLong", "(" + ctxDesc + ")J", false);
        } else if (returnType == Type.D) {
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "finishCallDouble", "(" + ctxDesc + ")D", false);
        } else if (returnType == Type.F) {
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "finishCallFloat", "(" + ctxDesc + ")F", false);
        } else {
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "finishCall", "(" + ctxDesc + ")" + Type.OBJECT, false);
        }
    }

    /**
     * 根据参数类型生成对应的 FunctionContext setter 调用
     */
    private static void emitSetArg(Type t, Type expected, MethodVisitor mv) {
        String ctxPath = FunctionContext.TYPE.getPath();
        if (t.isPrimitive()) {
            Type target = (expected != null && expected.isPrimitive()) ? expected : t;
            emitWideningConversion(t, target, mv);
            emitSetPrimitive(target, ctxPath, mv);
        } else if (expected != null && expected.isPrimitive()) {
            emitUnboxToPrimitive(expected, ctxPath, mv);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(I" + Type.OBJECT + ")V", false);
        }
    }

    /**
     * 生成 primitive 拓宽转换指令
     */
    private static void emitWideningConversion(Type from, Type to, MethodVisitor mv) {
        if (from == to) return;
        if (from == Type.I || from == Type.Z) {
            if (to == Type.J) mv.visitInsn(I2L);
            else if (to == Type.F) mv.visitInsn(I2F);
            else if (to == Type.D) mv.visitInsn(I2D);
        } else if (from == Type.J) {
            if (to == Type.F) mv.visitInsn(L2F);
            else if (to == Type.D) mv.visitInsn(L2D);
        } else if (from == Type.F) {
            if (to == Type.D) mv.visitInsn(F2D);
        }
    }

    /**
     * 生成 primitive setter 调用
     */
    private static void emitSetPrimitive(Type t, String ctxPath, MethodVisitor mv) {
        if (t == Type.I || t == Type.Z) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setInt", "(II)V", false);
        } else if (t == Type.J) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setLong", "(IJ)V", false);
        } else if (t == Type.F) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setFloat", "(IF)V", false);
        } else if (t == Type.D) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setDouble", "(ID)V", false);
        }
    }

    /**
     * 生成 Object -> primitive 拆箱并设置
     */
    private static void emitUnboxToPrimitive(Type expected, String ctxPath, MethodVisitor mv) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
        if (expected == Type.I || expected == Type.Z) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setInt", "(II)V", false);
        } else if (expected == Type.J) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setLong", "(IJ)V", false);
        } else if (expected == Type.F) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setFloat", "(IF)V", false);
        } else if (expected == Type.D) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setDouble", "(ID)V", false);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(I" + Type.OBJECT + ")V", false);
        }
    }

    @Override
    public void analyzeTypes(FunctionCallExpression result, TypeAnalyzer analyzer) {
        // 先分析所有参数
        ParseResult[] args = result.getArguments();
        for (ParseResult arg : args) {
            analyzer.analyzeNode(arg);
        }
        // 收集参数类型
        Type[] argTypes = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = analyzer.inferType(args[i]);
        }
        // 解析具体重载并设置索引
        FunctionPosition position = result.getPosition();
        if (position != null) {
            result.setResolvedPositionIndex(position.resolveIndex(argTypes));
        }
        // 基于 target 类型预解析扩展函数
        Type targetType = analyzer.getCurrentTargetType();
        if (targetType != null && targetType.getSource() != null) {
            result.resolveExtensionFunction(targetType.getSource(), argTypes);
        }
    }

    @Override
    public Type inferResultType(FunctionCallExpression result, TypeAnalyzer analyzer) {
        // 优先使用预解析的扩展函数
        Function resolvedExt = result.getResolvedExtensionFunction();
        if (resolvedExt != null) {
            return resolvedExt.getReturnType();
        }
        // 先收集参数类型
        ParseResult[] args = result.getArguments();
        Type[] argTypes = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = analyzer.inferType(args[i]);
        }
        // 尝试通过函数名和参数类型从全局运行时查找函数
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        OverloadSet overloadSet = runtime.getSystemFunctions().get(result.getFunctionName());
        if (overloadSet != null) {
            Function function = overloadSet.resolve(argTypes);
            if (function != null) {
                return function.getReturnType();
            }
        }
        // 尝试从扩展函数推断返回类型
        ExtensionFunctionPosition extPos = result.getExtensionPosition();
        if (extPos != null) {
            Type inferred = extPos.inferReturnType(argTypes);
            if (inferred != null) {
                return inferred;
            }
        }
        return Type.OBJECT;
    }

    /**
     * 将 primitive 值按目标类型设置到 FunctionContext
     */
    private static void setPrimitiveArg(FunctionContext<?> ctx, int i, Type target, Type source, long raw) {
        double value = readPrimitiveAsDouble(source, raw);
        switch (target.getDescriptor()) {
            case "I":
            case "Z":
                ctx.setInt(i, (int) value);
                break;
            case "J":
                ctx.setLong(i, (long) value);
                break;
            case "F":
                ctx.setFloat(i, (float) value);
                break;
            case "D":
                ctx.setDouble(i, value);
                break;
        }
    }

    /**
     * 从 raw bits 读取 primitive 值为 double
     */
    private static double readPrimitiveAsDouble(Type t, long raw) {
        switch (t.getDescriptor()) {
            case "I": case "Z": return (int) raw;
            case "J": return raw;
            case "F": return Float.intBitsToFloat((int) raw);
            case "D": return Double.longBitsToDouble(raw);
            default: return 0;
        }
    }

    /**
     * 从 Object 转换为 primitive 并设置到 FunctionContext
     */
    private static void setArgFromObject(FunctionContext<?> ctx, int i, Type expected, Object ref) {
        if (ref instanceof Number) {
            Number num = (Number) ref;
            if (expected == Type.I || expected == Type.Z) {
                ctx.setInt(i, num.intValue());
            } else if (expected == Type.J) {
                ctx.setLong(i, num.longValue());
            } else if (expected == Type.F) {
                ctx.setFloat(i, num.floatValue());
            } else if (expected == Type.D) {
                ctx.setDouble(i, num.doubleValue());
            }
        } else if (ref instanceof Boolean) {
            ctx.setInt(i, (Boolean) ref ? 1 : 0);
        } else {
            throw new ClassCastException("Cannot convert " + (ref == null ? "null" : ref.getClass().getName()) + " to " + expected);
        }
    }
}
