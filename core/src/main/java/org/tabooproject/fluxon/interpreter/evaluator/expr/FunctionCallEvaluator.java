package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.funccall.*;
import org.tabooproject.fluxon.interpreter.evaluator.expr.funccall.FunctionCallHandler.PrepareCallResult;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 函数调用表达式求值器
 *
 * @author sky
 */
public class FunctionCallEvaluator extends ExpressionEvaluator<FunctionCallExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public Type evaluate(Interpreter interpreter, FunctionCallExpression expr) {
        ParseResult[] args = expr.getArguments();
        int argCount = args.length;
        Environment env = interpreter.getEnvironment();
        // 快速路径：使用缓存的解析结果，跳过 selectHandler + resolveFunction
        // volatile 读取保证跨线程可见性，CachedResolution 不可变保证读到的字段值完整
        FunctionCallExpression.CachedResolution cached = expr.cachedResolution;
        if (cached != null) {
            Object target = env.getTarget();
            Class<?> targetClass = target != null ? target.getClass() : null;
            if (targetClass == cached.guardClass) {
                Function fn = cached.function;
                FunctionContextPool pool = interpreter.getPool();
                FunctionContext<?> ctx = pool.borrow(fn, target, argCount, env);
                try {
                    evaluateArguments(interpreter, ctx, args, argCount, cached.expectedTypes);
                } catch (Throwable ex) {
                    pool.releaseTop();
                    throw ex;
                }
                return FunctionCallHandlers.executeSync(interpreter, ctx, fn);
            }
        }
        // 慢速路径：完整解析
        FunctionCallHandler handler = selectHandler(interpreter, expr);
        FunctionContext<?> ctx = handler.prepareCall(interpreter, expr, argCount);
        boolean isDeferred = handler == DeferredOverloadHandler.INSTANCE || handler == DeferredExtensionHandler.INSTANCE;
        Type[] expectedTypes = getExpectedTypes(ctx.getFunction(), isDeferred);
        try {
            evaluateArguments(interpreter, ctx, args, argCount, expectedTypes);
        } catch (Throwable ex) {
            ctx.getPool().releaseTop();
            throw ex;
        }
        // 缓存非延迟、非异步的函数解析结果
        // volatile 写入保证 CachedResolution 的字段在引用发布前对其他线程完整可见
        if (!isDeferred) {
            Function resolved = ctx.getFunction();
            if (!resolved.isAsync() && !resolved.isPrimarySync()) {
                Object target = env.getTarget();
                Class<?> guard = target != null ? target.getClass() : null;
                expr.cachedResolution = new FunctionCallExpression.CachedResolution(resolved, expectedTypes, guard);
            }
        }
        return handler.finishCall(interpreter, expr, ctx);
    }

    private static void evaluateArguments(Interpreter interpreter, FunctionContext<?> ctx, ParseResult[] args, int argCount, Type[] expectedTypes) {
        for (int i = 0; i < argCount; i++) {
            Type t = interpreter.evaluate(args[i]);
            Type expected = (expectedTypes != null && i < expectedTypes.length) ? expectedTypes[i] : Type.OBJECT;
            FunctionCallHandlers.setArgument(interpreter, ctx, i, t, expected);
        }
    }

    @Override
    public Type generateBytecode(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv) {
        ParseResult[] args = expr.getArguments();
        int argCount = args.length;
        int savedLocalVar = ctx.getLocalVarIndex();
        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        Type[] argTypes = inferArgTypes(args, analyzer);
        // DirectBinding 快速路径：跳过整个 prepareCall/finishCall 框架
        Type directResult = tryGenerateDirectCall(expr, args, argTypes, analyzer, ctx, mv);
        if (directResult != null) {
            ctx.restoreLocalVarIndex(savedLocalVar);
            return directResult;
        }
        FunctionCallHandler handler = selectBytecodeHandler(expr, argTypes, analyzer, ctx);
        boolean isDeferred = handler == DeferredOverloadHandler.INSTANCE || handler == DeferredExtensionHandler.INSTANCE;
        // 延迟解析时不使用 expectedTypes，让运行时处理类型转换
        Type[] expectedTypes = isDeferred ? null : expr.resolveExpectedParameterTypes(argTypes);
        PrepareCallResult prepareResult = handler.generatePrepareCall(expr, ctx, mv, argCount);
        for (int i = 0; i < argCount; i++) {
            mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
            mv.visitLdcInsn(i);
            Evaluator<ParseResult> argEval = ctx.getEvaluator(args[i]);
            if (argEval == null) throw new EvaluatorNotFoundError("No evaluator found for argument expression");
            Type t = argEval.generateBytecode(args[i], ctx, mv);
            if (t == Type.VOID) throw new VoidError("Void type is not allowed for function arguments");
            Type expected = (expectedTypes != null && i < expectedTypes.length) ? expectedTypes[i] : null;
            FunctionCallHandlers.emitSetArg(t, expected, mv);
        }
        Type returnType = inferReturnType(expr, analyzer, isDeferred);
        Type actualReturn = handler.generateFinishCall(expr, ctx, mv, prepareResult, returnType);
        ctx.restoreLocalVarIndex(savedLocalVar);
        return actualReturn;
    }

    private FunctionCallHandler selectHandler(Interpreter interpreter, FunctionCallExpression expr) {
        // 无状态判断：不写入共享 AST，避免多线程缓存竞争
        ExtensionFunctionPosition extPos = expr.getExtensionPosition();
        if (extPos != null) {
            Object target = interpreter.getEnvironment().getTarget();
            if (target != null) {
                ExtensionDispatchTable dispatchTable = FluxonRuntime.getInstance().getCachedDispatchTables()[extPos.getIndex()];
                OverloadSet overloadSet = dispatchTable.resolveOverloadSet(target.getClass());
                if (overloadSet != null && overloadSet.size() > 1) {
                    return DeferredExtensionHandler.INSTANCE;
                }
            }
        }
        FunctionPosition position = expr.getPosition();
        if (position != null && position.getOverloadSet().size() > 1) {
            return DeferredOverloadHandler.INSTANCE;
        }
        return DynamicResolutionHandler.INSTANCE;
    }

    private FunctionCallHandler selectBytecodeHandler(FunctionCallExpression expr, Type[] argTypes, TypeAnalyzer analyzer, CodeContext ctx) {
        // 扩展函数已在编译时解析
        if (expr.getResolvedExtensionFunction() != null && expr.getResolvedTargetClass() != null) {
            return ResolvedExtensionHandler.INSTANCE;
        }
        int argCount = argTypes != null ? argTypes.length : expr.getArguments().length;
        // 检查扩展函数重载
        ExtensionFunctionPosition extPos = expr.getExtensionPosition();
        if (extPos != null && needsDeferredExtensionResolution(extPos, argCount, analyzer)) {
            return DeferredExtensionHandler.INSTANCE;
        }
        // 检查系统函数重载
        FunctionPosition position = expr.getPosition();
        if (position != null && position.getOverloadSet().size() > 1 && hasUnknownType(argTypes)) {
            return DeferredOverloadHandler.INSTANCE;
        }
        // 用户定义函数：编译期直接引用静态字段，跳过运行时名称查找
        if (ctx.getUserFunctionOwner(expr.getFunctionName()) != null) {
            return DirectFunctionHandler.INSTANCE;
        }
        return DynamicResolutionHandler.INSTANCE;
    }

    /**
     * 判断扩展函数是否需要延迟重载解析
     */
    private boolean needsDeferredExtensionResolution(ExtensionFunctionPosition extPos, int argCount, TypeAnalyzer analyzer) {
        Type targetType = analyzer != null ? analyzer.getCurrentTargetType() : null;
        boolean isUnknownTarget = targetType == null || targetType == Type.OBJECT;
        // target 类型未知时，检查所有 OverloadSet
        if (isUnknownTarget) {
            for (OverloadSet overloadSet : extPos.getOverloadSets().values()) {
                if (countMatchingOverloads(overloadSet, argCount) > 1) {
                    return true;
                }
            }
            return false;
        }
        // target 类型已知时，查找最具体的匹配 OverloadSet
        Class<?> targetClass = targetType.getSource();
        if (targetClass == null) {
            return false;
        }
        OverloadSet bestMatch = null;
        Class<?> bestClass = null;
        for (Map.Entry<Class<?>, OverloadSet> entry : extPos.getOverloadSets().entrySet()) {
            if (entry.getKey().isAssignableFrom(targetClass)) {
                if (bestClass == null || bestClass.isAssignableFrom(entry.getKey())) {
                    bestMatch = entry.getValue();
                    bestClass = entry.getKey();
                }
            }
        }
        return bestMatch != null && countMatchingOverloads(bestMatch, argCount) > 1;
    }

    /**
     * 统计参数数量匹配的重载个数
     */
    private int countMatchingOverloads(OverloadSet overloadSet, int argCount) {
        int count = 0;
        for (Function f : overloadSet.getOverloads()) {
            FunctionSignature sig = f.getSignature();
            if (sig == null || sig.getParameterCount() == argCount) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void analyzeTypes(FunctionCallExpression expr, TypeAnalyzer analyzer) {
        ParseResult[] args = expr.getArguments();
        for (ParseResult arg : args) {
            analyzer.analyzeNode(arg);
        }
        Type[] argTypes = inferArgTypes(args, analyzer);
        FunctionPosition position = expr.getPosition();
        if (position != null) {
            expr.setResolvedPositionIndex(position.resolveIndex(argTypes));
        }
        Type targetType = analyzer.getCurrentTargetType();
        if (targetType != null && targetType.getSource() != null) {
            expr.resolveExtensionFunction(targetType.getSource(), argTypes);
        }
    }

    @Override
    public Type inferResultType(FunctionCallExpression expr, TypeAnalyzer analyzer) {
        Function resolvedExt = expr.getResolvedExtensionFunction();
        if (resolvedExt != null) return resolvedExt.getReturnType();
        Type[] argTypes = inferArgTypes(expr.getArguments(), analyzer);
        OverloadSet overloadSet = FluxonRuntime.getInstance().getSystemFunctions().get(expr.getFunctionName());
        if (overloadSet != null) {
            Function function = overloadSet.resolve(argTypes);
            if (function != null) return function.getReturnType();
        }
        ExtensionFunctionPosition extPos = expr.getExtensionPosition();
        if (extPos != null) {
            Type inferred = extPos.inferReturnType(argTypes);
            if (inferred != null) return inferred;
        }
        return Type.OBJECT;
    }

    private Type[] getExpectedTypes(Function function, boolean isDeferred) {
        if (isDeferred || function.getSignature() == null) return null;
        return function.getSignature().getParameterTypes();
    }

    private Type[] inferArgTypes(ParseResult[] args, TypeAnalyzer analyzer) {
        if (analyzer == null) return null;
        Type[] types = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = analyzer.inferType(args[i]);
        }
        return types;
    }

    private boolean hasUnknownType(Type[] types) {
        if (types == null) return false;
        for (Type t : types) {
            if (t == Type.OBJECT) return true;
        }
        return false;
    }

    private Type inferReturnType(FunctionCallExpression expr, TypeAnalyzer analyzer, boolean isDeferred) {
        if (isDeferred || analyzer == null) return Type.OBJECT;
        Type inferred = inferResultType(expr, analyzer);
        return (inferred == Type.VOID) ? Type.OBJECT : inferred;
    }

    /**
     * 尝试生成 DirectBinding 内联调用
     * 如果函数有 DirectBinding 且可在编译期确定，直接生成 INVOKESTATIC，跳过整个调用框架
     *
     * @return 返回类型，null 表示不适用 DirectBinding
     */
    private Type tryGenerateDirectCall(
            FunctionCallExpression expr,
            ParseResult[] args,
            Type[] argTypes,
            TypeAnalyzer analyzer,
            CodeContext ctx,
            MethodVisitor mv
    ) {
        // 用户定义函数不走 DirectBinding
        if (ctx.getUserFunctionOwner(expr.getFunctionName()) != null) return null;
        // 尝试扩展函数 DirectBinding
        Function resolvedExt = expr.getResolvedExtensionFunction();
        if (resolvedExt != null && expr.getResolvedTargetClass() != null) {
            return tryGenerateDirectExtensionCall(resolvedExt, expr.getResolvedTargetClass(), args, argTypes, ctx, mv);
        }
        // 有 extensionPosition 但未解析到具体函数，不能 DirectBinding
        if (expr.getExtensionPosition() != null) return null;
        // 系统函数 DirectBinding
        OverloadSet overloadSet = FluxonRuntime.getInstance().getSystemFunctions().get(expr.getFunctionName());
        if (overloadSet == null) return null;
        // 多重载 + 存在未知类型参数时不能在编译期确定重载，fallback 到运行时
        if (overloadSet.size() > 1 && hasUnknownType(argTypes)) return null;
        Function function = overloadSet.resolve(argTypes != null ? argTypes : new Type[args.length]);
        if (function == null) return null;
        DirectBinding binding = function.getDirectBinding();
        if (binding == null) return null;
        FunctionSignature signature = function.getSignature();
        if (signature == null) return null;
        // async/primarySync 不能内联
        if (function.isAsync() || function.isPrimarySync()) return null;
        Type[] paramTypes = signature.getParameterTypes();
        // 生成参数求值 + 类型转换
        for (int i = 0; i < args.length; i++) {
            Evaluator<ParseResult> argEval = ctx.getEvaluator(args[i]);
            if (argEval == null) throw new EvaluatorNotFoundError("No evaluator found for argument expression");
            Type actual = argEval.generateBytecode(args[i], ctx, mv);
            if (actual == Type.VOID) throw new VoidError("Void type is not allowed for function arguments");
            Type expected = i < paramTypes.length ? paramTypes[i] : Type.OBJECT;
            emitDirectArgConversion(actual, expected, mv);
        }
        // INVOKESTATIC
        mv.visitMethodInsn(INVOKESTATIC, binding.getOwner(), binding.getMethod(), binding.getDescriptor(), false);
        return signature.getReturnType();
    }

    /**
     * 尝试为已解析的扩展函数生成 DirectBinding 调用
     * 生成序列：loadEnvironment → getTarget → CHECKCAST → 参数求值 → INVOKESTATIC
     */
    private Type tryGenerateDirectExtensionCall(
            Function function,
            Class<?> targetClass,
            ParseResult[] args,
            Type[] argTypes,
            CodeContext ctx,
            MethodVisitor mv
    ) {
        DirectBinding binding = function.getDirectBinding();
        if (binding == null) return null;
        FunctionSignature signature = function.getSignature();
        if (signature == null) return null;
        if (function.isAsync() || function.isPrimarySync()) return null;
        Type[] paramTypes = signature.getParameterTypes();
        // 加载 target：environment.getTarget() + CHECKCAST
        Instructions.loadEnvironment(mv, ctx);
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getTarget", "()" + Type.OBJECT, false);
        mv.visitTypeInsn(CHECKCAST, targetClass.getName().replace('.', '/'));
        // 生成参数求值 + 类型转换
        for (int i = 0; i < args.length; i++) {
            Evaluator<ParseResult> argEval = ctx.getEvaluator(args[i]);
            if (argEval == null) throw new EvaluatorNotFoundError("No evaluator found for argument expression");
            Type actual = argEval.generateBytecode(args[i], ctx, mv);
            if (actual == Type.VOID) throw new VoidError("Void type is not allowed for function arguments");
            Type expected = i < paramTypes.length ? paramTypes[i] : Type.OBJECT;
            emitDirectArgConversion(actual, expected, mv);
        }
        // INVOKESTATIC
        mv.visitMethodInsn(INVOKESTATIC, binding.getOwner(), binding.getMethod(), binding.getDescriptor(), false);
        return signature.getReturnType();
    }

    /**
     * 生成参数类型转换字节码（DirectBinding 专用）
     * 将栈顶值从 actual 类型转换为 expected 类型
     */
    private void emitDirectArgConversion(Type actual, Type expected, MethodVisitor mv) {
        if (actual == expected) return;
        // 原始类型之间的转换
        if (actual.isPrimitive() && expected.isPrimitive()) {
            FunctionCallHandlers.emitPrimitiveConversion(actual, expected, mv);
            return;
        }
        // Object → 原始类型：拆箱
        if (!actual.isPrimitive() && expected.isPrimitive()) {
            FunctionCallHandlers.emitUnbox(expected, mv);
            return;
        }
        // 原始类型 → Object：装箱
        if (actual.isPrimitive() && !expected.isPrimitive()) {
            FunctionCallHandlers.emitBox(actual, mv);
        }
    }
}
