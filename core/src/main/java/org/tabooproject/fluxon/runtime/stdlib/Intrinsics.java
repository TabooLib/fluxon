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
        DestructuringRegistry.getInstance().destructure(environment, variables, element);
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
        int startInt = ((Number) start).intValue();
        int endInt = ((Number) end).intValue();
        if (!isInclusive) {
            endInt += (startInt <= endInt) ? -1 : 1;
        }
        return new IntRange(startInt, endInt);
    }

    /**
     * 获取变量或函数
     *
     * @param environment 脚本运行环境
     * @param name        变量或函数名称
     * @param isOptional  是否为可选参数
     * @param index       索引
     * @return 变量或函数对象
     */
    public static Object getVariableOrFunction(Environment environment, String name, boolean isOptional, int index) {
        // 局部变量直接索引访问
        if (index >= 0) return environment.getLocalRef(index);
        // 根变量
        if (environment.hasRootVariable(name)) {
            return environment.getRootVariable(name);
        }
        // 获取函数
        Function fun = environment.getFunctionOrNull(name);
        if (fun != null) {
            return fun;
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
            FunctionContextPool pool = ctx.getPool();
            Interpreter child = interpreter != null ? interpreter.createChild() : null;
            ctx.setInterpreter(child);
            ctx.detachFromPool();
            return ThreadPoolManager.getInstance().submitAsync(() -> {
                try {
                    function.call(ctx);
                    return getReturnValue(ctx);
                } finally {
                    // 归还到原借出线程的池
                    if (pool != null) {
                        pool.returnFromOtherThread(ctx);
                    }
                }
            });
        } else if (function.isPrimarySync()) {
            FunctionContextPool pool = ctx.getPool();
            Interpreter child = interpreter != null ? interpreter.createChild() : null;
            ctx.setInterpreter(child);
            ctx.detachFromPool();
            CompletableFuture<Object> future = new CompletableFuture<>();
            FluxonRuntime.getInstance().getPrimaryThreadExecutor().execute(() -> {
                try {
                    function.call(ctx);
                    future.complete(getReturnValue(ctx));
                } catch (Throwable ex) {
                    if (AnnotationAccess.hasAnnotation(function, "except")) {
                        ex.printStackTrace();
                    }
                    future.completeExceptionally(ex);
                } finally {
                    // 归还到原借出线程的池
                    if (pool != null) {
                        pool.returnFromOtherThread(ctx);
                    }
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
            if (AnnotationAccess.hasAnnotation(function, "except")) {
                ex.printStackTrace();
            }
            throw ex;
        }
    }

    /**
     * 完成同步函数调用，返回 int
     */
    public static int finishCallInt(FunctionContext<?> ctx) {
        try {
            ctx.getFunction().call(ctx);
            int result = (int) ctx.getReturnPrimitive();
            ctx.close();
            return result;
        } catch (Throwable ex) {
            ctx.close();
            throw ex;
        }
    }

    /**
     * 完成同步函数调用，返回 long
     */
    public static long finishCallLong(FunctionContext<?> ctx) {
        try {
            ctx.getFunction().call(ctx);
            long result = ctx.getReturnPrimitive();
            ctx.close();
            return result;
        } catch (Throwable ex) {
            ctx.close();
            throw ex;
        }
    }

    /**
     * 完成同步函数调用，返回 double
     */
    public static double finishCallDouble(FunctionContext<?> ctx) {
        try {
            ctx.getFunction().call(ctx);
            double result = Double.longBitsToDouble(ctx.getReturnPrimitive());
            ctx.close();
            return result;
        } catch (Throwable ex) {
            ctx.close();
            throw ex;
        }
    }

    /**
     * 完成同步函数调用，返回 float
     */
    public static float finishCallFloat(FunctionContext<?> ctx) {
        try {
            ctx.getFunction().call(ctx);
            float result = Float.intBitsToFloat((int) ctx.getReturnPrimitive());
            ctx.close();
            return result;
        } catch (Throwable ex) {
            ctx.close();
            throw ex;
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
        switch (t.getDescriptor()) {
            case "I": return (int) raw;
            case "J": return raw;
            case "D": return Double.longBitsToDouble(raw);
            case "F": return Float.intBitsToFloat((int) raw);
            case "Z": return raw != 0;
        }
        return ctx.getReturnRef();
    }

    /**
     * 解析函数引用，若找不到则抛出 FunctionNotFoundError
     */
    public static Function resolveFunction(Environment environment, Object target, String name, int argCount, int pos, int exPos) {
        Function function = resolveFunctionOrNull(environment, target, name, pos, exPos);
        if (function == null) {
            throw new FunctionNotFoundError(environment, target, name, argCount, pos, exPos);
        }
        return function;
    }

    /**
     * 尝试解析函数引用，若找不到则返回 null
     * 编译期已确定 pos，运行时直接用位置获取
     */
    private static Function resolveFunctionOrNull(Environment environment, Object target, String name, int pos, int exPos) {
        Function function = null;
        if (target != null && target != GlobalObject.INSTANCE && exPos != -1) {
            function = environment.getExtensionFunctionOrNull(target.getClass(), exPos);
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
            // 如果是 CompletableFuture，等待其完成并返回结果
            try {
                return ((CompletableFuture<?>) value).get(AWAIT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (InterruptedException | ExecutionException e) {
                throw new IntrinsicException("Error while awaiting future: " + e.getMessage(), e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        } else if (value instanceof Future<?>) {
            // 如果是普通的 Future，等待其完成并返回结果
            try {
                return ((Future<?>) value).get(AWAIT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (InterruptedException | ExecutionException e) {
                throw new IntrinsicException("Error while awaiting future: " + e.getMessage(), e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        // 如果不是异步类型，直接返回值
        return value;
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
        for (Map.Entry<String, Integer> entry : parameters.entrySet()) {
            final int slot = entry.getValue();
            final Object value = (slot >= 0 && slot < len) ? context.getArgBoxed(slot) : null;
            functionEnv.setLocalRef(slot, value);
            functionEnv.getLocalVariableNames()[slot] = entry.getKey();
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
        } catch (Exception ex) {
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
        } catch (Exception ex) {
            throw new RuntimeException("Error executing domain '" + domainName + "': " + ex.getMessage(), ex);
        }
    }
}
