package org.tabooproject.fluxon.jsr223;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.util.Map;

/**
 * Bindings 与 Environment 之间的双向转换工具
 * 提供高效的变量注入和提取功能
 */
final class BindingsHelper {

    private BindingsHelper() {
        // 工具类禁止实例化
    }

    /**
     * 将 ScriptContext 中的变量注入到 Environment
     * 遵循 GLOBAL_SCOPE < ENGINE_SCOPE 的优先级规则
     *
     * @param env     目标环境
     * @param context 脚本上下文
     */
    public static void injectFromContext(Environment env, ScriptContext context) {
        if (context == null) {
            return;
        }
        // 注入 GLOBAL_SCOPE 变量（优先级低）
        injectBindings(env, context.getBindings(ScriptContext.GLOBAL_SCOPE));
        // 注入 ENGINE_SCOPE 变量（优先级高，会覆盖 GLOBAL_SCOPE）
        injectBindings(env, context.getBindings(ScriptContext.ENGINE_SCOPE));
    }

    /**
     * 将 Bindings 中的变量注入到 Environment
     * 优化：快速跳过空 bindings，减少字符串比较开销
     *
     * @param env      目标环境
     * @param bindings 绑定容器
     */
    public static void injectBindings(Environment env, Bindings bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            String key = entry.getKey();
            // 快速跳过内部属性（优化字符串比较）
            if (isInternalKey(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Function) {
                env.defineRootFunction(key, (Function) value);
            } else {
                env.defineRootVariable(key, value);
            }
        }
    }

    /**
     * 检查是否为内部属性 key
     */
    private static boolean isInternalKey(String key) {
        int len = key.length();
        if (len < 7) return false;
        char c = key.charAt(0);
        // fluxon.* 或 javax.script.*
        return (c == 'f' && len > 7 && key.startsWith("fluxon."))
            || (c == 'j' && len > 13 && key.startsWith("javax.script."));
    }

    /**
     * 从 Environment 提取变量到 ScriptContext 的 ENGINE_SCOPE
     *
     * @param env     源环境
     * @param context 目标上下文
     */
    public static void extractToContext(Environment env, ScriptContext context) {
        if (context == null) {
            return;
        }
        Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineBindings == null) {
            return;
        }
        Map<String, Object> vars = env.getRootVariables();
        if (vars != null && !vars.isEmpty()) {
            engineBindings.putAll(vars);
        }
        Map<String, Function> funcs = env.getRootFunctions();
        if (funcs != null && !funcs.isEmpty()) {
            engineBindings.putAll(funcs);
        }
    }

    /**
     * 创建带有变量的新环境
     */
    public static Environment createEnvironment(ScriptContext context) {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        injectFromContext(env, context);
        return env;
    }
}
