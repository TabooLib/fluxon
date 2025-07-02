package org.tabooproject.fluxon.runtime.stdlib;

import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class Operations {

    public static final Type TYPE = new Type(Operations.class);

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
        Math.checkNumberOperands(start, end);

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
     * @param callee     被调用的对象
     * @param arguments  参数数组
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
}