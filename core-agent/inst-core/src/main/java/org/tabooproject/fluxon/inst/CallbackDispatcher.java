package org.tabooproject.fluxon.inst;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 回调分发器。
 * 被注入代码调用的静态入口点，将调用分发给对应的 Fluxon 函数。
 */
public class CallbackDispatcher {

    public static final Type TYPE = new Type(CallbackDispatcher.class);

    private static final Logger LOGGER = Logger.getLogger(CallbackDispatcher.class.getName());

    /** 回调标记：跳过原方法执行 */
    public static final Object SKIP = new Object();

    /** 回调标记：继续原方法执行 */
    public static final Object PROCEED = new Object();

    /** 注入 ID -> 回调函数 */
    private static final Map<String, Function> callbacks = new ConcurrentHashMap<>();

    /** 注入 ID -> 执行环境 */
    private static final Map<String, Environment> environments = new ConcurrentHashMap<>();

    /** 注入 ID -> 原方法调用器（用于 replace 模式的 original() 调用） */
    private static final Map<String, OriginalMethodInvoker> originalInvokers = new ConcurrentHashMap<>();

    /**
     * 原方法调用器接口。
     */
    @FunctionalInterface
    public interface OriginalMethodInvoker {
        Object invoke(Object[] args) throws Throwable;
    }

    /**
     * 注册回调函数。
     * 
     * @param specId 注入规格 ID
     * @param callback 回调函数
     * @param environment 执行环境
     */
    public static void register(String specId, Function callback, Environment environment) {
        callbacks.put(specId, callback);
        environments.put(specId, environment);
    }

    /**
     * 注销回调函数。
     */
    public static void unregister(String specId) {
        callbacks.remove(specId);
        environments.remove(specId);
        originalInvokers.remove(specId);
    }

    /**
     * 分发回调调用。
     * 由注入的字节码调用。
     * 
     * @param specId 注入规格 ID
     * @param args 方法参数（对于实例方法，args[0] 是 this）
     * @return 回调返回值，或 PROCEED 表示继续执行原方法
     */
    public static Object dispatch(String specId, Object[] args) {
        Function callback = callbacks.get(specId);
        Environment environment = environments.get(specId);
        if (callback == null || environment == null) {
            LOGGER.warning("未找到回调: " + specId);
            return PROCEED;
        }
        try {
            // 创建 FunctionContext 并调用 Fluxon 函数
            FunctionContext<?> context = new FunctionContext<>(callback, null, args, environment);
            Object result = callback.call(context);
            return result != null ? result : PROCEED;
        } catch (Exception e) {
            LOGGER.severe("回调执行失败: " + specId + ", 错误: " + e.getMessage());
            throw new RuntimeException("Fluxon 注入回调执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分发 BEFORE advice 回调。
     * 
     * @param specId 注入规格 ID
     * @param args 方法参数
     * @return true 表示继续执行原方法，false 表示跳过
     */
    public static boolean dispatchBefore(String specId, Object[] args) {
        Object result = dispatch(specId, args);
        return result != SKIP;
    }

    /**
     * 分发 REPLACE 回调。
     * 
     * @param specId 注入规格 ID
     * @param args 方法参数
     * @return 替换方法的返回值
     */
    public static Object dispatchReplace(String specId, Object[] args) {
        return dispatch(specId, args);
    }

    /**
     * 调用原方法（用于 replace 模式中的 original() 调用）。
     * 
     * @param specId 注入规格 ID
     * @param args 原方法参数
     * @return 原方法返回值
     */
    public static Object invokeOriginal(String specId, Object[] args) throws Throwable {
        OriginalMethodInvoker invoker = originalInvokers.get(specId);
        if (invoker == null) {
            throw new IllegalStateException("未找到原方法调用器: " + specId);
        }
        return invoker.invoke(args);
    }

    /**
     * 检查是否有指定 ID 的回调。
     */
    public static boolean hasCallback(String specId) {
        return callbacks.containsKey(specId);
    }

    /**
     * 清除所有回调（用于测试）。
     */
    public static void clearAll() {
        callbacks.clear();
        environments.clear();
        originalInvokers.clear();
    }
}
