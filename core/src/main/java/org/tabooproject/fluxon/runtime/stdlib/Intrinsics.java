package org.tabooproject.fluxon.runtime.stdlib;

import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.parser.expression.WhenExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class Intrinsics {

    public static final Type TYPE = new Type(Intrinsics.class);

    /**
     * 为集合对象创建迭代器
     *
     * @param collection 集合对象
     * @return 迭代器对象
     * @throws RuntimeException 如果对象不可迭代
     */
    public static Iterator<?> createIterator(Object collection) {
        if (collection instanceof List) {
            return ((List<?>) collection).iterator();
        } else if (collection instanceof Map) {
            return ((Map<?, ?>) collection).entrySet().iterator();
        } else if (collection instanceof Iterable) {
            return ((Iterable<?>) collection).iterator();
        } else if (collection instanceof Object[]) {
            return Arrays.asList((Object[]) collection).iterator();
        } else if (collection != null) {
            throw new RuntimeException("Cannot iterate over " + collection.getClass().getName());
        } else {
            throw new RuntimeException("Cannot iterate over null");
        }
    }

    /**
     * 执行解构操作并设置环境变量
     *
     * @param scriptBase 运行时脚本基础类
     * @param variables  变量名列表（序列化为字符串数组）
     * @param element    要解构的元素
     */
    public static void destructureAndSetVars(RuntimeScriptBase scriptBase, String[] variables, Object element) {
        DestructuringRegistry.getInstance().destructure(scriptBase.getEnvironment(), Arrays.asList(variables), element);
    }

    /**
     * 创建数字范围列表
     *
     * @param start       开始值
     * @param end         结束值
     * @param isInclusive 是否包含结束值
     * @return 范围列表
     * @throws RuntimeException 如果操作数不是数字类型
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

        // 创建范围结果列表
        List<Integer> rangeList = new java.util.ArrayList<>();
        // 支持正向和反向范围
        if (startInt <= endInt) {
            // 正向范围
            for (int i = startInt; i <= endInt; i++) {
                rangeList.add(i);
            }
        } else {
            // 反向范围
            for (int i = startInt; i >= endInt; i--) {
                rangeList.add(i);
            }
        }
        return rangeList;
    }

    /**
     * 执行函数调用
     *
     * @param environment 脚本运行环境
     * @param callee      被调用的对象
     * @param arguments   参数数组
     * @return 函数调用结果
     */
    public static Object callFunction(Environment environment, Object callee, Object[] arguments) {
        // 获取函数
        Function function;
        if (callee instanceof Function) {
            function = ((Function) callee);
        } else {
            function = environment.getFunction(callee.toString());
        }

        if (function.isAsync()) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return function.call(arguments);
                } catch (Throwable e) {
                    throw new RuntimeException("Error while executing async function: " + e.getMessage(), e);
                }
            });
        } else {
            return function.call(arguments);
        }
    }

    /**
     * 等待异步值完成并返回结果
     *
     * @param value 要等待的值（可能是 CompletableFuture、Future 或普通值）
     * @return 异步操作的结果，如果不是异步类型则直接返回值
     * @throws RuntimeException 如果等待过程中发生错误
     */
    public static Object awaitValue(Object value) {
        if (value instanceof CompletableFuture<?>) {
            // 如果是 CompletableFuture，等待其完成并返回结果
            try {
                return ((CompletableFuture<?>) value).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error while awaiting future: " + e.getMessage(), e);
            }
        } else if (value instanceof Future<?>) {
            // 如果是普通的 Future，等待其完成并返回结果
            try {
                return ((Future<?>) value).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error while awaiting future: " + e.getMessage(), e);
            }
        }
        // 如果不是异步类型，直接返回值
        return value;
    }

    /**
     * 为函数调用绑定参数到新环境中
     * 参考 UserFunction 的参数绑定逻辑
     *
     * @param parentEnv  父环境
     * @param parameters 参数名列表
     * @param args       参数值数组
     * @return 绑定了参数的新环境
     */
    public static Environment bindFunctionParameters(Environment parentEnv, String[] parameters, Object[] args) {
        // 创建新的环境，父环境为传入的环境
        Environment functionEnv = new Environment(parentEnv);
        // 绑定参数
        if (parameters != null && args != null) {
            int minParamCount = java.lang.Math.min(parameters.length, args.length);
            // 绑定实际传递的参数
            for (int i = 0; i < minParamCount; i++) {
                functionEnv.defineVariable(parameters[i], args[i]);
            }
            // 未传递的参数赋值为 null
            for (int i = minParamCount; i < parameters.length; i++) {
                functionEnv.defineVariable(parameters[i], null);
            }
        } else if (parameters != null) {
            // 如果没有参数值，所有参数都设为 null
            for (String parameter : parameters) {
                functionEnv.defineVariable(parameter, null);
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
}