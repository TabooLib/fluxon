package org.tabooproject.fluxon.runtime.stdlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.parser.CommandExecutor;
import org.tabooproject.fluxon.parser.CommandHandler;
import org.tabooproject.fluxon.parser.DomainExecutor;
import org.tabooproject.fluxon.parser.expression.WhenExpression;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.collection.IntRange;
import org.tabooproject.fluxon.runtime.concurrent.ThreadPoolManager;
import org.tabooproject.fluxon.runtime.error.ArgumentTypeMismatchError;
import org.tabooproject.fluxon.runtime.error.FunctionNotFoundError;
import org.tabooproject.fluxon.runtime.error.IndexAccessError;
import org.tabooproject.fluxon.runtime.error.VariableNotFoundError;
import org.tabooproject.fluxon.runtime.index.IndexAccessorRegistry;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.lang.reflect.Array.*;

public final class Intrinsics {

    public static final Type TYPE = new Type(Intrinsics.class);

    /**
     * @deprecated await 等待脚本异步流程时不再主动超时，该字段仅保留二进制兼容。
     */
    @Deprecated
    public static long AWAIT_TIMEOUT_MINUTES = 1;

    private static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * 为集合对象创建迭代器
     *
     * @param collection 集合对象
     * @return 迭代器对象
     * @throws IntrinsicException 如果对象不可迭代
     */
    public static Iterator<?> createIterator(Object collection) {
        if (collection instanceof Iterable) {
            return ((Iterable<?>) collection).iterator();
        } else if (collection instanceof Map) {
            return ((Map<?, ?>) collection).entrySet().iterator();
        } else if (collection instanceof Object[]) {
            return Arrays.asList((Object[]) collection).iterator();
        } else if (collection instanceof String) {
            return new Iterator<String>() {
                private final String str = (String) collection;
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < str.length();
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return String.valueOf(str.charAt(index++));
                }
            };
        } else if (collection != null) {
            throw new IntrinsicException("Cannot iterate over " + collection.getClass().getName());
        } else {
            throw new IntrinsicException("Cannot iterate over null");
        }
    }

    /**
     * 执行解构操作并设置环境变量
     * 此方法通过字节码调用
     *
     * @param scriptBase 运行时脚本基础类
     * @param variables  变量名列表（序列化为字符串数组）
     * @param element    要解构的元素
     */
    public static void destructure(RuntimeScriptBase scriptBase, Map<String, Integer> variables, Object element) {
        Environment environment = scriptBase.getEnvironment();
        DestructuringRegistry.getInstance().destructure(environment, variables, element, scriptBase::getVariableType);
    }

    /**
     * 创建数字范围列表
     *
     * @param start       开始值
     * @param end         结束值
     * @param isInclusive 是否包含结束值
     * @return 范围列表
     * @throws IntrinsicException 如果操作数不是数字类型
     */
    public static IntRange createRange(Object start, Object end, boolean isInclusive) {
        Operations.checkNumberOperands(start, end);
        return createRange(((Number) start).intValue(), ((Number) end).intValue(), isInclusive);
    }

    /**
     * 创建数字范围列表（primitive 快速路径，避免装箱）
     */
    public static IntRange createRange(int start, int end, boolean isInclusive) {
        if (!isInclusive) {
            end += (start <= end) ? -1 : 1;
        }
        return new IntRange(start, end);
    }

    /**
     * 获取变量
     *
     * @param environment 脚本运行环境
     * @param name        变量名称
     * @param isOptional  是否为可选参数
     * @param index       索引
     * @return 变量对象
     */
    public static Object getVariable(Environment environment, String name, boolean isOptional, int index) {
        // 局部变量直接索引访问
        if (index >= 0) return environment.getLocalRef(index);
        // 根变量
        if (environment.hasRootVariable(name)) {
            return environment.getRootVariable(name);
        }
        if (isOptional) {
            return null;
        }
        throw new VariableNotFoundError(environment, name, Arrays.asList(environment.getLocalVariableNames()));
    }

    /**
     * 准备函数调用：解析函数并从池中借用 FunctionContext
     *
     * @param pool        函数上下文池
     * @param environment 脚本运行环境
     * @param name        函数名称
     * @param argCount    参数数量
     * @param pos         函数位置（编译期确定）
     * @param exPos       扩展函数位置
     * @return 准备好的 FunctionContext
     */
    public static FunctionContext<?> prepareCall(FunctionContextPool pool, Environment environment, String name, int argCount, int pos, int exPos) {
        if (pool == null) pool = FunctionContextPool.local();
        Object target = environment.getTarget();
        Function function = resolveFunction(environment, target, name, argCount, pos, exPos);
        return pool.borrow(function, target, argCount, environment);
    }

    /**
     * 直接准备函数调用：跳过动态解析，直接使用编译期已解析的函数
     *
     * @param pool        函数上下文池
     * @param environment 脚本运行环境
     * @param function    编译期已解析的函数
     * @param argCount    参数数量
     * @return 准备好的 FunctionContext
     */
    public static FunctionContext<?> prepareCallDirect(FunctionContextPool pool, Environment environment, Function function, int argCount) {
        if (pool == null) pool = FunctionContextPool.local();
        Object target = environment.getTarget();
        return pool.borrow(function, target, argCount, environment);
    }

    /**
     * 准备延迟重载解析的函数调用
     * 当编译期无法确定具体重载时使用，运行时根据实际参数类型选择
     *
     * @param pool        函数上下文池
     * @param environment 脚本运行环境
     * @param overloadSet 重载集合
     * @param argCount    参数数量
     * @return 准备好的 FunctionContext
     */
    public static FunctionContext<?> prepareCallDeferred(FunctionContextPool pool, Environment environment, OverloadSet overloadSet, int argCount) {
        if (pool == null) pool = FunctionContextPool.local();
        Object target = environment.getTarget();
        // 使用参数数量匹配的重载作为占位
        Function placeholder = overloadSet.resolveByArgCount(argCount);
        if (placeholder == null) {
            placeholder = overloadSet.first();
        }
        return pool.borrow(Objects.requireNonNull(placeholder), target, argCount, environment);
    }

    /**
     * 准备延迟解析的扩展函数调用
     *
     * @param pool        函数上下文池
     * @param environment 脚本运行环境
     * @param target      目标对象
     * @param extPosIndex 扩展函数位置索引
     * @param argCount    参数数量
     * @return 准备好的 FunctionContext
     */
    public static FunctionContext<?> prepareCallDeferredExtension(FunctionContextPool pool, Environment environment, Object target, int extPosIndex, int argCount) {
        if (pool == null) pool = FunctionContextPool.local();
        // 使用参数数量匹配的重载作为占位
        ExtensionDispatchTable dispatchTable = FluxonRuntime.getInstance().getCachedDispatchTables()[extPosIndex];
        Function placeholder = dispatchTable.resolve(target.getClass(), argCount);
        if (placeholder == null) {
            OverloadSet overloadSet = dispatchTable.resolveOverloadSet(target.getClass());
            placeholder = overloadSet != null ? overloadSet.first() : null;
        }
        return pool.borrow(Objects.requireNonNull(placeholder), target, argCount, environment);
    }

    /**
     * 完成同步函数调用（编译期已确认非 async/primarySync 时使用）
     * 跳过 isAsync/isPrimarySync 检查，减少虚方法调用和分支预测开销
     */
    public static Object finishCallSync(FunctionContext<?> ctx) {
        FunctionContextPool pool = ctx.getPool();
        try {
            ctx.getFunction().call(ctx);
            Object result = getReturnValue(ctx);
            pool.releaseTop();
            return result;
        } catch (Throwable ex) {
            pool.releaseTop();
            throw ex;
        }
    }

    /**
     * 完成函数调用（无 interpreter）
     */
    public static Object finishCall(FunctionContext<?> ctx) {
        return finishCall(ctx, null);
    }

    /**
     * 完成函数调用：处理 sync/async/primarySync
     */
    public static Object finishCall(FunctionContext<?> ctx, @Nullable Interpreter interpreter) {
        Function function = ctx.getFunction();
        if (function.isAsync()) {
            ctx.detachFromPool();
            return ThreadPoolManager.getInstance().submitAsync(() -> {
                ctx.reassignPool();
                if (interpreter != null) {
                    ctx.setInterpreter(interpreter.createChild());
                }
                try {
                    function.call(ctx);
                    return getReturnValue(ctx);
                } catch (Throwable ex) {
                    if (isCancellation(ex)) throw ex;
                    if (AnnotationAccess.hasAnnotation(function, "except")) {
                        ex.printStackTrace();
                        throw ex;
                    }
                    return null;
                }
            });
        } else if (function.isPrimarySync()) {
            ctx.detachFromPool();
            CompletableFuture<Object> future = new CompletableFuture<>();
            FluxonRuntime.getInstance().getPrimaryThreadExecutor().execute(() -> {
                ctx.reassignPool();
                if (interpreter != null) {
                    ctx.setInterpreter(interpreter.createChild());
                }
                try {
                    function.call(ctx);
                    future.complete(getReturnValue(ctx));
                } catch (Throwable ex) {
                    if (AnnotationAccess.hasAnnotation(function, "except") && !isCancellation(ex)) ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
            return future;
        }
        // sync
        try {
            ctx.setInterpreter(interpreter);
            function.call(ctx);
            Object result = getReturnValue(ctx);
            ctx.close();
            return result;
        } catch (Throwable ex) {
            ctx.close();
            if (AnnotationAccess.hasAnnotation(function, "except") && !isCancellation(ex)) ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * 完成同步函数调用，返回 int
     */
    public static int finishCallInt(FunctionContext<?> ctx) {
        FunctionContextPool pool = ctx.getPool();
        try {
            ctx.getFunction().call(ctx);
            int result = (int) ctx.getReturnPrimitive();
            pool.releaseTop();
            return result;
        } catch (Throwable ex) {
            pool.releaseTop();
            throw ex;
        }
    }

    /**
     * 完成同步函数调用，返回 long
     */
    public static long finishCallLong(FunctionContext<?> ctx) {
        FunctionContextPool pool = ctx.getPool();
        try {
            ctx.getFunction().call(ctx);
            long result = ctx.getReturnPrimitive();
            pool.releaseTop();
            return result;
        } catch (Throwable ex) {
            pool.releaseTop();
            throw ex;
        }
    }

    /**
     * 完成同步函数调用，返回 double
     */
    public static double finishCallDouble(FunctionContext<?> ctx) {
        FunctionContextPool pool = ctx.getPool();
        try {
            ctx.getFunction().call(ctx);
            double result = Double.longBitsToDouble(ctx.getReturnPrimitive());
            pool.releaseTop();
            return result;
        } catch (Throwable ex) {
            pool.releaseTop();
            throw ex;
        }
    }

    /**
     * 完成同步函数调用，返回 float
     */
    public static float finishCallFloat(FunctionContext<?> ctx) {
        FunctionContextPool pool = ctx.getPool();
        try {
            ctx.getFunction().call(ctx);
            float result = Float.intBitsToFloat((int) ctx.getReturnPrimitive());
            pool.releaseTop();
            return result;
        } catch (Throwable ex) {
            pool.releaseTop();
            throw ex;
        }
    }

    /**
     * 完成延迟解析的函数调用（编译模式使用）
     * 首次调用时解析并缓存，后续调用直接使用缓存
     *
     * @param ctx         函数上下文
     * @param overloadSet 重载集合
     * @param cache       缓存数组
     * @param slot        缓存槽位
     * @return 函数返回值
     */
    public static Object finishCallDeferred(FunctionContext<?> ctx, OverloadSet overloadSet, Function[] cache, int slot) {
        Function resolved = cache[slot];
        Type[] argTypes = ctx.collectArgTypes();
        if (resolved == null) {
            resolved = overloadSet.resolve(argTypes);
            if (resolved != null) {
                cache[slot] = resolved;
            } else {
                resolved = ctx.getFunction(); // fallback
            }
        }
        ctx.setFunctionAndConvertArgs(resolved, argTypes);
        try {
            resolved.call(ctx);
            return getReturnValue(ctx);
        } finally {
            ctx.close();
        }
    }

    /**
     * 完成延迟解析的扩展函数调用（编译模式使用）
     *
     * @param ctx         函数上下文
     * @param extPosIndex 扩展函数位置索引
     * @return 函数返回值
     */
    public static Object finishCallDeferredExtension(FunctionContext<?> ctx, int extPosIndex) {
        Object target = ctx.getTarget();
        Type[] argTypes = ctx.collectArgTypes();
        ExtensionDispatchTable dispatchTable = FluxonRuntime.getInstance().getCachedDispatchTables()[extPosIndex];
        Function resolved = dispatchTable.resolve(target.getClass(), argTypes);
        if (resolved == null) {
            resolved = ctx.getFunction(); // fallback
        }
        ctx.setFunctionAndConvertArgs(resolved, argTypes);
        try {
            resolved.call(ctx);
            return getReturnValue(ctx);
        } finally {
            ctx.close();
        }
    }

    /**
     * 从 FunctionContext 中获取返回值，支持原始类型
     */
    private static Object getReturnValue(FunctionContext<?> ctx) {
        Type t = ctx.getReturnType();
        if (t == null || t == Type.OBJECT || !t.isPrimitive()) {
            return ctx.getReturnRef();
        }
        long raw = ctx.getReturnPrimitive();
        if (t == Type.I) return (int) raw;
        if (t == Type.J) return raw;
        if (t == Type.D) return Double.longBitsToDouble(raw);
        if (t == Type.F) return Float.intBitsToFloat((int) raw);
        if (t == Type.Z) return raw != 0;
        return ctx.getReturnRef();
    }

    /**
     * 解析函数引用，若找不到则抛出 FunctionNotFoundError
     */
    public static Function resolveFunction(Environment environment, Object target, String name, int argCount, int pos, int exPos) {
        Function function = resolveFunctionOrNull(environment, target, name, argCount, pos, exPos);
        if (function == null) {
            throw new FunctionNotFoundError(environment, target, name, argCount, pos, exPos);
        }
        return function;
    }

    /**
     * 尝试解析函数引用，若找不到则返回 null
     * 编译期已确定 pos，运行时直接用位置获取
     */
    private static Function resolveFunctionOrNull(Environment environment, Object target, String name, int argCount, int pos, int exPos) {
        Function function = null;
        if (target != null && target != GlobalObject.INSTANCE && exPos != -1) {
            function = environment.getExtensionFunctionOrNull(target.getClass(), exPos, argCount);
        }
        if (function == null) {
            if (pos != -1) {
                function = environment.getRootSystemFunctions()[pos];
            } else {
                function = environment.getFunctionOrNull(name);
            }
        }
        return function;
    }

    /**
     * 等待异步值完成并返回结果
     *
     * @param value 要等待的值（可能是 CompletableFuture、Future 或普通值）
     * @return 异步操作的结果，如果不是 asynchronous 类型则直接返回值
     * @throws IntrinsicException 如果等待过程中发生错误
     */
    public static Object awaitValue(Object value) {
        if (value instanceof CompletableFuture<?>) {
            return awaitFuture((CompletableFuture<?>) value);
        } else if (value instanceof Future<?>) {
            return awaitFuture((Future<?>) value);
        }
        // 如果不是异步类型，直接返回值
        return value;
    }

    private static Object awaitFuture(Future<?> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntrinsicException("Interrupted while awaiting future", e);
        } catch (ExecutionException e) {
            if (isCancellation(e.getCause())) {
                throw (CancellationException) e.getCause();
            }
            throw new IntrinsicException("Error while awaiting future: " + e.getMessage(), e);
        }
    }

    private static boolean isCancellation(Throwable ex) {
        if (ex instanceof CancellationException) {
            return true;
        }
        if (ex instanceof CompletionException || ex instanceof ExecutionException) {
            return ex.getCause() != null && isCancellation(ex.getCause());
        }
        return false;
    }

    /**
     * 为函数调用绑定参数到新环境中
     *
     * @param parentEnv      父环境
     * @param parameters     参数名到 slot 的映射
     * @param context        函数上下文（从中读取参数）
     * @param localVariables 局部变量数量
     * @return 绑定了参数的新环境
     */
    @NotNull
    public static Environment bindFunctionParameters(@NotNull Environment parentEnv, Map<String, Integer> parameters, @NotNull FunctionContext<?> context, int localVariables) {
        Environment functionEnv = new Environment(parentEnv, localVariables);
        if (parameters == null || parameters.isEmpty()) {
            return functionEnv;
        }
        final int len = context.getArgumentCount();
        int argIndex = 0;
        for (Map.Entry<String, Integer> entry : parameters.entrySet()) {
            final int slot = entry.getValue();
            final Object value = (argIndex < len) ? context.getArgBoxed(argIndex) : null;
            functionEnv.setLocalRef(slot, value);
            functionEnv.getLocalVariableNames()[slot] = entry.getKey();
            argIndex++;
        }
        return functionEnv;
    }

    /**
     * 为方法绑定参数（简化版，用于匿名类方法）
     *
     * @param parentEnv 父环境
     * @param names     参数名数组
     * @param args      参数值数组
     * @return 绑定了参数的新环境
     */
    @NotNull
    public static Environment bindMethodParameters(@NotNull Environment parentEnv, @NotNull String[] names, @NotNull Object[] args, int localVarCount) {
        Environment env = new Environment(parentEnv, names.length + localVarCount);
        for (int i = 0; i < names.length && i < args.length; i++) {
            env.setLocalRef(i, args[i]);
            env.getLocalVariableNames()[i] = names[i];
        }
        return env;
    }

    /**
     * 执行 When 分支匹配判断
     *
     * @param subject     主题对象（可能为 null）
     * @param condition   条件对象
     * @param matchType   匹配类型
     * @param targetClass IS 类型匹配时的目标类（可为 null）
     * @return 是否匹配成功
     */
    public static boolean matchWhenBranch(Object subject, Object condition, WhenExpression.MatchType matchType, Class<?> targetClass) {
        switch (matchType) {
            case EQUAL:
                if (subject != null) {
                    return Operations.isEqual(subject, condition);
                } else {
                    return Operations.isTrue(condition);
                }
            case CONTAINS:
                return checkContains(subject, condition, false);
            case NOT_CONTAINS:
                return checkContains(subject, condition, true);
            case IS:
                return isInstanceOf(subject, targetClass);
            default:
                return false;
        }
    }

    /**
     * 检查包含关系
     *
     * @param subject   主题对象
     * @param condition 条件对象
     * @param negate    是否取反（用于 NOT_CONTAINS）
     * @return 包含关系判断结果
     */
    private static boolean checkContains(Object subject, Object condition, boolean negate) {
        if (subject == null || condition == null) {
            return negate; // null 情况下，CONTAINS 返回 false，NOT_CONTAINS 返回 true
        }
        boolean contains = false;
        if (condition instanceof List) {
            contains = ((List<?>) condition).contains(subject);
        } else if (condition instanceof Map) {
            contains = ((Map<?, ?>) condition).containsKey(subject);
        } else if (condition instanceof String && subject instanceof String) {
            contains = ((String) condition).contains((String) subject);
        }
        return negate != contains;
    }

    /**
     * 批量类型检查
     */
    public static void checkArgumentTypes(FunctionContext<?> context, Class<?>[] expect, Object[] args) {
        for (int i = 0; i < expect.length; i++) {
            if (args.length <= i || args[i] == null) {
                continue;
            }
            if (!isCompatibleType(expect[i], args[i])) {
                throw new ArgumentTypeMismatchError(context, i, expect[i], args[i]);
            }
        }
    }

    /**
     * 检查值是否兼容期望的类型（支持基本类型和包装类型的互相匹配）
     *
     * @param expectedType 期望的类型
     * @param value        实际值
     * @return 是否兼容
     */
    public static boolean isCompatibleType(Class<?> expectedType, Object value) {
        Class<?> actualType = value != null ? value.getClass() : null;
        return TypeCompatibility.isTypeCompatible(expectedType, actualType);
    }

    /**
     * 设置索引访问的值
     *
     * @param target 目标对象（列表、映射或数组）
     * @param index  索引对象
     * @param value  要设置的值
     * @throws IndexAccessError 如果索引无效或目标类型不支持索引设置
     */
    @SuppressWarnings("unchecked")
    public static void setIndex(Object target, Object index, Object value) {
        if (target == null) throw IndexAccessError.nullTarget(index);
        if (target instanceof List) {
            int idx = ((Number) index).intValue();
            List<Object> list = (List<Object>) target;
            if (idx < 0 || idx >= list.size()) {
                throw IndexAccessError.outOfBounds(target, index, list.size());
            }
            list.set(idx, value);
        } else if (target instanceof Map) {
            ((Map<Object, Object>) target).put(index, value);
        } else if (target instanceof Object[]) {
            int idx = ((Number) index).intValue();
            Object[] arr = (Object[]) target;
            if (idx < 0 || idx >= arr.length) {
                throw IndexAccessError.outOfBounds(target, index, arr.length);
            }
            arr[idx] = value;
        } else if (target.getClass().isArray()) {
            // 处理基本类型数组 (int[], long[], double[], etc.)
            int idx = ((Number) index).intValue();
            int length = getLength(target);
            if (idx < 0 || idx >= length) {
                throw IndexAccessError.outOfBounds(target, index, length);
            }
            set(target, idx, value);
        } else {
            // 尝试使用第三方注册的索引访问器
            IndexAccessorRegistry.AccessResult result = IndexAccessorRegistry.getInstance().trySet(target, index, value);
            if (result != null) {
                if (!result.isSuccess()) {
                    throw new IntrinsicException("Index set failed: " + result.getError().getMessage(), result.getError());
                }
                return;
            }
            throw IndexAccessError.unsupportedSetType(target, index);
        }
    }

    /**
     * 执行单次索引访问
     *
     * @param target 目标对象（列表、映射、字符串或数组）
     * @param index  索引对象（必须是数字）
     * @return 索引对应的值
     * @throws IndexAccessError 如果索引无效或目标类型不支持索引访问
     */
    public static Object getIndex(Object target, Object index) {
        if (target == null) throw IndexAccessError.nullTarget(index);
        if (target instanceof List) {
            int idx = ((Number) index).intValue();
            List<?> list = (List<?>) target;
            if (idx < 0 || idx >= list.size()) {
                throw IndexAccessError.outOfBounds(target, index, list.size());
            }
            return list.get(idx);
        } else if (target instanceof Map) {
            return ((Map<?, ?>) target).get(index);
        } else if (target instanceof String) {
            int idx = ((Number) index).intValue();
            String str = (String) target;
            if (idx < 0 || idx >= str.length()) {
                throw IndexAccessError.outOfBounds(target, index, str.length());
            }
            return String.valueOf(str.charAt(idx));
        } else if (target instanceof Object[]) {
            int idx = ((Number) index).intValue();
            Object[] arr = (Object[]) target;
            if (idx < 0 || idx >= arr.length) {
                throw IndexAccessError.outOfBounds(target, index, arr.length);
            }
            return arr[idx];
        } else if (target.getClass().isArray()) {
            // 处理基本类型数组 (int[], long[], double[], etc.)
            int idx = ((Number) index).intValue();
            int length = getLength(target);
            if (idx < 0 || idx >= length) {
                throw IndexAccessError.outOfBounds(target, index, length);
            }
            return get(target, idx);
        } else {
            // 尝试使用第三方注册的索引访问器
            IndexAccessorRegistry.AccessResult result = IndexAccessorRegistry.getInstance().tryGet(target, index);
            if (result != null) {
                if (!result.isSuccess()) {
                    throw new IntrinsicException("Index access failed: " + result.getError().getMessage(), result.getError());
                }
                return result.getValue();
            }
            throw IndexAccessError.unsupportedType(target, index);
        }
    }

    /**
     * 类型检查：判断对象是否为指定类型的实例
     *
     * @param obj         要检查的对象
     * @param targetClass 目标类型
     * @return 是否为该类型的实例
     */
    public static boolean isInstanceOf(Object obj, Class<?> targetClass) {
        if (obj == null) {
            return false;
        }
        return targetClass.isInstance(obj);
    }

    /**
     * 执行 Command
     *
     * @param commandName 命令名称
     * @param environment 运行时环境
     * @param parsedData  解析时捕获的数据
     * @return 命令的返回值
     */
    @SuppressWarnings("unchecked")
    public static Object executeCommand(String commandName, Environment environment, Object parsedData) {
        CommandHandler<?> handler = environment.getCommandRegistry().get(commandName);
        if (handler == null) {
            throw new RuntimeException("Command not found: " + commandName);
        }
        try {
            CommandExecutor<Object> executor = (CommandExecutor<Object>) handler.getExecutor();
            return executor.execute(environment, parsedData);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException("Error executing command '" + commandName + "': " + ex.getMessage(), ex);
        }
    }

    /**
     * 执行域（编译模式入口）
     * <p>
     * 此方法由编译后的字节码调用，从 DomainRegistry 获取执行器并执行。
     *
     * @param domainName  域名称
     * @param environment 运行时环境
     * @param bodyFunc    编译后的域体函数
     * @return 域的返回值
     */
    public static Object executeDomain(String domainName, Environment environment, Function bodyFunc) {
        DomainExecutor executor = environment.getDomainRegistry().get(domainName);
        if (executor == null) {
            throw new RuntimeException("Domain not found: " + domainName);
        }
        try {
            Supplier<Object> body = () -> {
                FunctionContextPool pool = FunctionContextPool.local();
                try (FunctionContext<?> ctx = pool.borrow(bodyFunc, null, EMPTY_ARGS, environment)) {
                    bodyFunc.call(ctx);
                    return ctx.getReturnRef();
                }
            };
            return executor.execute(environment, body);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException("Error executing domain '" + domainName + "': " + ex.getMessage(), ex);
        }
    }
}
