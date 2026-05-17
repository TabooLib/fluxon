package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.funccall.*;
import org.tabooproject.fluxon.interpreter.evaluator.expr.funccall.FunctionCallHandler.PrepareCallResult;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;

import java.util.Map;

import static org.objectweb.asm.Opcodes.ALOAD;

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
        FunctionSignature sig = ctx.getFunction().getSignature();
        Type[] expectedTypes = (isDeferred || sig == null) ? null : sig.getParameterTypes();
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
                // 使用 ctx 中已捕获的 target 而非再次读 env.getTarget()，
                // 避免并发修改 env.target 导致 function/guardClass 不匹配
                Object target = ctx.getTarget();
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

    /**
     * 解释模式 handler 选择：根据运行时 target 和重载信息选择调用策略
     */
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
                if (overloadSet != null) {
                    return DynamicResolutionHandler.INSTANCE;
                }
            }
        }
        if (expr.isDirectContextCall()) {
            return DynamicResolutionHandler.INSTANCE;
        }
        FunctionPosition position = expr.getPosition();
        if (position != null && position.getOverloadSet().size() > 1) {
            return DeferredOverloadHandler.INSTANCE;
        }
        return DynamicResolutionHandler.INSTANCE;
    }

    @Override
    public Type generateBytecode(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv) {
        ParseResult[] args = expr.getArguments();
        int argCount = args.length;
        int savedLocalVar = ctx.getLocalVarIndex();
        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        Type[] argTypes = inferArgTypes(args, analyzer);
        // DirectBinding 快速路径：跳过整个 prepareCall/finishCall 框架
        Type directResult = DirectBindingEmitter.tryEmit(expr, args, argTypes, ctx, mv);
        if (directResult != null) {
            ctx.restoreLocalVarIndex(savedLocalVar);
            return directResult;
        }
        // 框架路径：selectHandler → prepareCall → 参数求值 → finishCall
        FunctionCallHandler handler = selectBytecodeHandler(expr, argTypes, analyzer, ctx);
        boolean isDeferred = handler == DeferredOverloadHandler.INSTANCE || handler == DeferredExtensionHandler.INSTANCE;
        Type[] expectedTypes = isDeferred ? null : expr.resolveExpectedParameterTypes(argTypes);
        PrepareCallResult prepareResult = handler.generatePrepareCall(expr, ctx, mv, argCount);
        for (int i = 0; i < argCount; i++) {
            mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
            mv.visitLdcInsn(i);
            Type t = FunctionCallHandlers.emitArgExpression(args[i], ctx, mv);
            Type expected = (expectedTypes != null && i < expectedTypes.length) ? expectedTypes[i] : null;
            FunctionCallHandlers.emitSetArg(t, expected, mv);
        }
        Type returnType = Type.OBJECT;
        if (!isDeferred && analyzer != null) {
            Type inferred = inferResultType(expr, analyzer);
            if (inferred != Type.VOID) returnType = inferred;
        }
        Type actualReturn = handler.generateFinishCall(expr, ctx, mv, prepareResult, returnType);
        ctx.restoreLocalVarIndex(savedLocalVar);
        return actualReturn;
    }

    /**
     * 编译模式 handler 选择：根据编译期类型信息选择最优路径
     */
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
        if (extPos != null && hasMatchingExtensionTarget(extPos, analyzer)) {
            return DynamicResolutionHandler.INSTANCE;
        }
        if (expr.isDirectContextCall()) {
            return DynamicResolutionHandler.INSTANCE;
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

    private boolean hasMatchingExtensionTarget(ExtensionFunctionPosition extPos, TypeAnalyzer analyzer) {
        Type targetType = analyzer != null ? analyzer.getCurrentTargetType() : null;
        if (targetType == null || targetType.getSource() == null || targetType == Type.OBJECT) {
            return false;
        }
        Class<?> targetClass = targetType.getSource();
        for (Class<?> extensionClass : extPos.getOverloadSets().keySet()) {
            if (extensionClass.isAssignableFrom(targetClass)) {
                return true;
            }
        }
        return false;
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
}
