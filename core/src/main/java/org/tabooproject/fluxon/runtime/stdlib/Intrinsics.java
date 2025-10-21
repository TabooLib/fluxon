package org.tabooproject.fluxon.runtime.stdlib;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.interpreter.error.ArgumentTypeMismatchException;
import org.tabooproject.fluxon.interpreter.error.FunctionNotFoundError;
import org.tabooproject.fluxon.interpreter.error.IndexAccessException;
import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;
import org.tabooproject.fluxon.parser.expression.WhenExpression;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.concurrent.ThreadPoolManager;

import java.util.*;
import java.util.concurrent.*;

public final class Intrinsics {

    public static final Type TYPE = new Type(Intrinsics.class);

    public static long AWAIT_TIMEOUT_MINUTES = 1;

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
    public static List<Integer> createRange(Object start, Object end, boolean isInclusive) {
        // 检查开始值和结束值是否为数字
        Operations.checkNumberOperands(start, end);

        // 转换为整数
        int startInt = ((Number) start).intValue();
        int endInt = ((Number) end).intValue();
        // 检查范围是否为包含上界类型
        if (!isInclusive) {
            endInt--;
        }

        // 计算所需的确切大小
        int size = Math.abs(endInt - startInt) + 1;
        // 创建具有预设容量的 ArrayList
        List<Integer> rangeList = new ArrayList<>(size);
        // 填充列表
        if (startInt <= endInt) {
            for (int i = startInt; i <= endInt; i++) {
                rangeList.add(i);
            }
        } else {
            for (int i = startInt; i >= endInt; i--) {
                rangeList.add(i);
            }
        }
        return rangeList;
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
        // 如果变量被定义
        // 无论变量值是否为空，都返回变量值
        if (environment.has(name, index)) {
            return environment.get(name, index);
        }
        // 获取变量
        Object var = environment.get(name, index);
        if (var != null) {
            return var;
        }
        // 获取函数
        Function fun = environment.getFunctionOrNull(name);
        if (fun != null) {
            return fun;
        }
        if (isOptional) {
            return null;
        }
        throw new VariableNotFoundException(name);
    }

    /**
     * 执行函数调用
     *
     * @param environment 脚本运行环境
     * @param name        函数名称
     * @param arguments   参数数组
     * @param pos         函数位置
     * @param exPos       扩展函数位置
     * @return 函数调用结果
     */
    public static Object callFunction(Environment environment, String name, Object[] arguments, int pos, int exPos) {
        // 获取调用目标
        Object target = environment.getTarget();
        // 获取函数
        Function function = null;
        // 优先尝试从扩展函数中获取函数
        if (target != null && target != GlobalObject.INSTANCE && exPos != -1) {
            // 此时有可能获取不到扩展函数
            // 例如：&sender::location()::isBehand( player(HUAIHEI)::location() )
            // player 在 isBehand 里调用，因此会尝试检索扩展函数
            function = environment.getExtensionFunctionOrNull(target.getClass(), name, exPos);
        }
        if (function == null) {
            if (pos != -1) {
                function = environment.getRootSystemFunctions()[pos];
            } else {
                function = environment.getFunctionOrNull(name);
            }
        }
        // 如果函数不存在
        if (function == null) {
            throw new FunctionNotFoundError(environment, target, name, arguments, pos, exPos);
        }
        final Function finalFunction = function;
        final FunctionContext<?> context = new FunctionContext<>(function, target, arguments, environment);
        if (function.isAsync()) {
            return ThreadPoolManager.getInstance().submitAsync(() -> {
                try {
                    return finalFunction.call(context);
                } catch (Throwable ex) {
                    ex.printStackTrace(); // 打印 async 的异常
                    throw ex;
                }
            });
        } else if (function.isPrimarySync()) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            FluxonRuntime.getInstance().getPrimaryThreadExecutor().execute(() -> {
                try {
                    future.complete(finalFunction.call(context));
                } catch (Throwable ex) {
                    ex.printStackTrace(); // 打印 sync 的异常
                    throw ex;
                }
            });
            return future;
        } else {
            return function.call(context);
        }
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
     * 参考 UserFunction 的参数绑定逻辑
     *
     * @param parentEnv      父环境
     * @param parameters     参数名列表
     * @param args           参数值数组
     * @param localVariables 局部变量数量
     * @return 绑定了参数的新环境
     */
    @NotNull
    public static Environment bindFunctionParameters(@NotNull Environment parentEnv, Map<String, Integer> parameters, @NotNull Object[] args, int localVariables) {
        // 创建函数环境
        Environment functionEnv = new Environment(parentEnv, localVariables);
        // 绑定参数
        if (!parameters.isEmpty() && args != null) {
            // 绑定实际传递的参数
            int index = 0;
            for (Map.Entry<String, Integer> entry : parameters.entrySet()) {
                if (index < args.length) {
                    functionEnv.assign(entry.getKey(), args[index], entry.getValue());
                } else {
                    functionEnv.assign(entry.getKey(), null, entry.getValue());
                }
                index++;
            }
        } else if (!parameters.isEmpty()) {
            // 如果没有参数值，所有参数都设为 null
            for (Map.Entry<String, Integer> entry : parameters.entrySet()) {
                functionEnv.assign(entry.getKey(), null, entry.getValue());
            }
        }
        return functionEnv;
    }

    /**
     * 执行 When 分支匹配判断
     *
     * @param subject   主题对象（可能为 null）
     * @param condition 条件对象
     * @param matchType 匹配类型
     * @return 是否匹配成功
     */
    public static boolean matchWhenBranch(Object subject, Object condition, WhenExpression.MatchType matchType) {
        switch (matchType) {
            case EQUAL:
                // 如果有主题，判断主题和条件是否相等
                if (subject != null) {
                    return Operations.isEqual(subject, condition);
                } else {
                    // 没有主题时，直接判断条件是否为真
                    return Operations.isTrue(condition);
                }
            case CONTAINS:
                return checkContains(subject, condition, false);
            case NOT_CONTAINS:
                return checkContains(subject, condition, true);
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
                throw new ArgumentTypeMismatchException(context, i, expect[i], args[i]);
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
        if (value == null) {
            return !expectedType.isPrimitive();
        }
        Class<?> actualType = value.getClass();
        // 直接类型匹配
        if (expectedType == actualType) {
            return true;
        }
        // 处理基本类型和包装类型的匹配
        if (expectedType.isPrimitive()) {
            if (expectedType == int.class) return actualType == Integer.class;
            if (expectedType == long.class) return actualType == Long.class;
            if (expectedType == double.class) return actualType == Double.class;
            if (expectedType == float.class) return actualType == Float.class;
            if (expectedType == boolean.class) return actualType == Boolean.class;
            if (expectedType == byte.class) return actualType == Byte.class;
            if (expectedType == short.class) return actualType == Short.class;
            if (expectedType == char.class) return actualType == Character.class;
            return false;
        }
        // 继承/实现关系检查（ArrayList 是 List 的子类，应该兼容）
        // expectedType.isAssignableFrom(actualType) 检查：actualType 能否赋值给 expectedType
        // 例如：List.class.isAssignableFrom(ArrayList.class) = true
        return expectedType.isAssignableFrom(actualType);
    }

    /**
     * 设置索引访问的值
     *
     * @param target 目标对象（列表、映射或数组）
     * @param index  索引对象
     * @param value  要设置的值
     * @throws IndexAccessException 如果索引无效或目标类型不支持索引设置
     */
    @SuppressWarnings("unchecked")
    public static void setIndex(Object target, Object index, Object value) {
        if (target == null) throw IndexAccessException.nullTarget(index);
        if (target instanceof List) {
            int idx = ((Number) index).intValue();
            List<Object> list = (List<Object>) target;
            if (idx < 0 || idx >= list.size()) {
                throw IndexAccessException.outOfBounds(target, index, list.size());
            }
            list.set(idx, value);
        } else if (target instanceof Map) {
            ((Map<Object, Object>) target).put(index, value);
        } else if (target instanceof Object[]) {
            int idx = ((Number) index).intValue();
            Object[] arr = (Object[]) target;
            if (idx < 0 || idx >= arr.length) {
                throw IndexAccessException.outOfBounds(target, index, arr.length);
            }
            arr[idx] = value;
        } else {
            throw IndexAccessException.unsupportedSetType(target, index);
        }
    }

    /**
     * 执行单次索引访问
     *
     * @param target 目标对象（列表、映射、字符串或数组）
     * @param index  索引对象（必须是数字）
     * @return 索引对应的值
     * @throws IndexAccessException 如果索引无效或目标类型不支持索引访问
     */
    public static Object getIndex(Object target, Object index) {
        if (target == null) throw IndexAccessException.nullTarget(index);
        if (target instanceof List) {
            int idx = ((Number) index).intValue();
            List<?> list = (List<?>) target;
            if (idx < 0 || idx >= list.size()) {
                throw IndexAccessException.outOfBounds(target, index, list.size());
            }
            return list.get(idx);
        } else if (target instanceof Map) {
            return ((Map<?, ?>) target).get(index);
        } else if (target instanceof String) {
            int idx = ((Number) index).intValue();
            String str = (String) target;
            if (idx < 0 || idx >= str.length()) {
                throw IndexAccessException.outOfBounds(target, index, str.length());
            }
            return String.valueOf(str.charAt(idx));
        } else if (target instanceof Object[]) {
            int idx = ((Number) index).intValue();
            Object[] arr = (Object[]) target;
            if (idx < 0 || idx >= arr.length) {
                throw IndexAccessException.outOfBounds(target, index, arr.length);
            }
            return arr[idx];
        } else {
            throw IndexAccessException.unsupportedType(target, index);
        }
    }
}