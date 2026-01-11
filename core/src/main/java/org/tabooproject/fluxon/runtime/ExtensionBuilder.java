package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ExtensionBuilder<Target> {

    private final FluxonRuntime runtime;
    private final Class<Target> extensionClass;
    private final String namespace;

    public ExtensionBuilder(FluxonRuntime runtime, Class<Target> extensionClass, String namespace) {
        this.runtime = runtime;
        this.extensionClass = extensionClass;
        this.namespace = namespace;
    }

    public ExtensionBuilder<Target> function(String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerExtensionFunction(extensionClass, namespace, name, paramCount, implementation);
        return this;
    }

    public ExtensionBuilder<Target> function(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerExtensionFunction(extensionClass, namespace, name, paramCounts, implementation);
        return this;
    }

    public ExtensionBuilder<Target> asyncFunction(String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerAsyncExtensionFunction(extensionClass, namespace, name, paramCount, implementation);
        return this;
    }

    public ExtensionBuilder<Target> asyncFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerAsyncExtensionFunction(extensionClass, namespace, name, paramCounts, implementation);
        return this;
    }

    public ExtensionBuilder<Target> syncFunction(String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerSyncExtensionFunction(extensionClass, namespace, name, paramCount, implementation);
        return this;
    }

    public ExtensionBuilder<Target> syncFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerSyncExtensionFunction(extensionClass, namespace, name, paramCounts, implementation);
        return this;
    }

    public Class<Target> getExtensionClass() {
        return extensionClass;
    }

    public String getNamespace() {
        return namespace;
    }

    /**
     * 函数式接口：处理集合元素和函数调用结果
     */
    @FunctionalInterface
    public interface IterableProcessor {
        void process(Object element, Object callResult);
    }

    /**
     * 函数式接口：测试集合元素和函数调用结果
     */
    @FunctionalInterface
    public interface IterablePredicate {
        boolean test(Object element, Object callResult);
    }

    /**
     * 函数式接口：比较两个对象
     */
    @FunctionalInterface
    public interface IterableComparator {
        boolean shouldReplace(Object current, Object candidate);
    }

    /**
     * 辅助方法：遍历集合中的每个元素，应用 closure 并处理结果
     * 在 function() 的实现中使用此方法来简化 lambda 遍历逻辑
     *
     * @param context   函数上下文
     * @param processor 处理器，接收原始元素和 closure 调用结果
     */
    @SuppressWarnings("unchecked")
    public static void forEachElement(FunctionContext<?> context, @Nullable IterableProcessor processor) {
        Iterable<Object> iterable = (Iterable<Object>) Objects.requireNonNull(context.getTarget());
        Function closure = context.getFunction(0);
        FunctionContextPool pool = context.getPool();
        try (FunctionContext<?> ctx = pool.borrowCopy(context, null)) {
            int index = 0;
            for (Object element : iterable) {
                Object callResult = closure.call(ctx.updateArguments(2, element, index, null, null));
                if (processor != null) {
                    processor.process(element, callResult);
                }
                index++;
            }
        }
    }

    /**
     * 辅助方法：遍历集合中的每个元素，应用 closure 并测试结果，支持短路
     * 在 function() 的实现中使用此方法来简化布尔测试逻辑
     *
     * @param context   函数上下文
     * @param predicate 断言，返回 false 时停止遍历
     * @return 如果所有元素都通过测试返回 true，否则返回 false
     */
    @SuppressWarnings("unchecked")
    public static boolean testElements(FunctionContext<?> context, IterablePredicate predicate) {
        Iterable<Object> iterable = (Iterable<Object>) Objects.requireNonNull(context.getTarget());
        Function closure = context.getFunction(0);
        FunctionContextPool pool = context.getPool();
        try (FunctionContext<?> ctx = pool.borrowCopy(context, null)) {
            int index = 0;
            for (Object element : iterable) {
                Object callResult = closure.call(ctx.updateArguments(2, element, index, null, null));
                if (!predicate.test(element, callResult)) {
                    return false;
                }
                index++;
            }
            return true;
        }
    }

    /**
     * 辅助方法：遍历集合中的每个元素，应用 closure 并比较结果，找到极值
     * 在 function() 的实现中使用此方法来简化 min/max 逻辑
     *
     * @param context    函数上下文
     * @param comparator 比较器，决定是否用新值替换当前值
     * @return 极值，如果集合为空或所有元素不可比较则返回 null
     */
    @SuppressWarnings("unchecked")
    public static Object compareElements(FunctionContext<?> context, IterableComparator comparator) {
        Iterable<Object> iterable = (Iterable<Object>) Objects.requireNonNull(context.getTarget());
        Function closure = context.getFunction(0);
        FunctionContextPool pool = context.getPool();
        try (FunctionContext<?> ctx = pool.borrowCopy(context, null)) {
            Object result = null;
            int index = 0;
            for (Object element : iterable) {
                Object callResult = closure.call(ctx.updateArguments(2, element, index, null, null));
                // 只处理 Comparable 类型
                if (callResult instanceof Comparable) {
                    if (result == null) {
                        result = callResult;
                    } else if (comparator.shouldReplace(result, callResult)) {
                        result = callResult;
                    }
                }
                index++;
            }
            return result;
        }
    }

    /**
     * 辅助方法：遍历集合中的每个元素，应用 closure 并比较结果，找到产生极值的元素
     * 与 compareElements 不同，此方法返回原始元素而非选择器的计算结果
     *
     * @param context    函数上下文
     * @param comparator 比较器，决定是否用新值替换当前值
     * @return 产生极值的元素，如果集合为空或所有元素不可比较则返回 null
     */
    @SuppressWarnings("unchecked")
    public static Object compareElementsBy(FunctionContext<?> context, IterableComparator comparator) {
        Iterable<Object> iterable = (Iterable<Object>) Objects.requireNonNull(context.getTarget());
        Function closure = context.getFunction(0);
        FunctionContextPool pool = context.getPool();
        try (FunctionContext<?> ctx = pool.borrowCopy(context, null)) {
            Object resultElement = null;
            Object resultValue = null;
            int index = 0;
            for (Object element : iterable) {
                Object callResult = closure.call(ctx.updateArguments(2, element, index, null, null));
                // 只处理 Comparable 类型
                if (callResult instanceof Comparable) {
                    if (resultValue == null) {
                        resultElement = element;
                        resultValue = callResult;
                    } else if (comparator.shouldReplace(resultValue, callResult)) {
                        resultElement = element;
                        resultValue = callResult;
                    }
                }
                index++;
            }
            return resultElement;
        }
    }
}
