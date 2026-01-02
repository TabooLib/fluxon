package org.tabooproject.fluxon.inst.function;

import org.tabooproject.fluxon.inst.CallbackDispatcher;
import org.tabooproject.fluxon.inst.InjectionRegistry;
import org.tabooproject.fluxon.inst.InjectionSpec;
import org.tabooproject.fluxon.inst.InjectionType;
import org.tabooproject.fluxon.runtime.*;

import java.util.*;

/**
 * fs:jvm 模块 - 动态字节码注入 API。
 *
 * <pre>{@code
 * import 'fs:jvm'
 *
 * id = jvm::inject("com.example.Foo::bar", [
 *     type: "replace",
 *     handler: |self, arg| { return "replaced" }
 * ])
 *
 * jvm::restore(id)          // 撤销注入
 * jvm::injections()     // 列出所有注入
 * }</pre>
 */
public final class FunctionJvm {

    public static final JvmModule INSTANCE = new JvmModule();

    private FunctionJvm() {
    }

    // ==================== 模块初始化 ====================

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("fs:jvm", "jvm", 0, ctx -> INSTANCE);
        runtime.registerExtensionFunction(JvmModule.class, "fs:jvm", "inject", 2, FunctionJvm::inject);
        runtime.registerExtensionFunction(JvmModule.class, "fs:jvm", "restore", 1, FunctionJvm::restore);
        runtime.registerExtensionFunction(JvmModule.class, "fs:jvm", "injections", 0, FunctionJvm::injections);
    }

    // ==================== API 实现 ====================

    /**
     * jvm()::inject(target, spec)
     */
    private static Object inject(FunctionContext<?> ctx) {
        TargetMethod method = parseTarget(Objects.requireNonNull(ctx.getString(0)));
        InjectionConfig config = parseSpec(ctx.getArgument(1));
        InjectionSpec spec = new InjectionSpec(method.className, method.methodName, method.descriptor, config.type);
        CallbackDispatcher.register(spec.getId(), config.callback, ctx.getEnvironment());
        return InjectionRegistry.getInstance().register(spec);
    }

    /**
     * jvm()::restore(idOrTarget)
     */
    private static Object restore(FunctionContext<?> ctx) {
        String idOrTarget = ctx.getString(0);
        // 尝试作为 ID 撤销
        if (InjectionRegistry.getInstance().unregister(idOrTarget)) {
            CallbackDispatcher.unregister(idOrTarget);
            return true;
        }
        // 尝试作为目标撤销
        if (Objects.requireNonNull(idOrTarget).contains("::")) {
            TargetMethod method = parseTarget(idOrTarget);
            return InjectionRegistry.getInstance().unregisterByTarget(method.className, method.methodName, method.descriptor);
        }
        return false;
    }

    /**
     * jvm()::injections()
     */
    private static Object injections(FunctionContext<?> ctx) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (InjectionSpec spec : InjectionRegistry.getInstance().getAllSpecs()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", spec.getId());
            item.put("target", spec.getTarget());
            item.put("type", spec.getType().name().toLowerCase());
            result.add(item);
        }
        return result;
    }

    // ==================== 解析逻辑 ====================

    @SuppressWarnings("unchecked")
    private static InjectionConfig parseSpec(Object spec) {
        if (spec instanceof Function) {
            return new InjectionConfig((Function) spec, InjectionType.BEFORE);
        }
        if (spec instanceof Map) {
            return parseSpecMap((Map<String, Object>) spec);
        }
        throw new IllegalArgumentException("spec 必须是 Function 或 Map 类型");
    }

    private static InjectionConfig parseSpecMap(Map<String, Object> map) {
        // 优先使用 handler 字段
        Object handler = map.get("handler");
        if (handler != null) {
            String typeStr = (String) map.getOrDefault("type", "before");
            InjectionType type = "replace".equalsIgnoreCase(typeStr) ? InjectionType.REPLACE : InjectionType.BEFORE;
            return new InjectionConfig(asFunction(handler), type);
        }
        // 兼容 before/replace 作为 key
        Object before = map.get("before");
        if (before != null) {
            return new InjectionConfig(asFunction(before), InjectionType.BEFORE);
        }
        Object replace = map.get("replace");
        if (replace != null) {
            return new InjectionConfig(asFunction(replace), InjectionType.REPLACE);
        }
        throw new IllegalArgumentException("spec 必须包含 handler、before 或 replace 字段");
    }

    private static Function asFunction(Object obj) {
        if (obj instanceof Function) {
            return (Function) obj;
        }
        throw new IllegalArgumentException("回调必须是 Function 类型");
    }

    private static TargetMethod parseTarget(String target) {
        int sep = target.indexOf("::");
        if (sep == -1) {
            throw new IllegalArgumentException("目标格式错误，应为 className::methodName");
        }
        String className = target.substring(0, sep).replace('.', '/');
        String methodPart = target.substring(sep + 2);
        int paren = methodPart.indexOf('(');
        if (paren != -1) {
            return new TargetMethod(className, methodPart.substring(0, paren), methodPart.substring(paren));
        }
        return new TargetMethod(className, methodPart, null);
    }

    // ==================== 内部类型 ====================

    /**
     * JVM 模块类型标记
     */
    public static final class JvmModule {}

    /**
     * 注入配置
     */
    private static final class InjectionConfig {
        final Function callback;
        final InjectionType type;

        InjectionConfig(Function callback, InjectionType type) {
            this.callback = callback;
            this.type = type;
        }
    }

    /**
     * 目标方法信息
     */
    private static final class TargetMethod {
        final String className;
        final String methodName;
        final String descriptor;

        TargetMethod(String className, String methodName, String descriptor) {
            this.className = className;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }
    }
}
