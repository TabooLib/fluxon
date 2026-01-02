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
 * id = jvm::inject("com.example.Foo::bar", "replace", |self, arg| {
 *     return "replaced"
 * })
 *
 * jvm::restore(id)      // 撤销注入
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
        runtime.registerExtensionFunction(JvmModule.class, "fs:jvm", "inject", Arrays.asList(2, 3), FunctionJvm::inject);
        runtime.registerExtensionFunction(JvmModule.class, "fs:jvm", "restore", 1, FunctionJvm::restore);
        runtime.registerExtensionFunction(JvmModule.class, "fs:jvm", "injections", 0, FunctionJvm::injections);
    }

    // ==================== API 实现 ====================

    /**
     * jvm()::inject(target, type, handler)
     * <p>
     * ctx.arg[0] target   - 目标方法 "com.example.Foo::bar" 或 "com.example.Foo::bar(Ljava/lang/String;)V"
     * ctx.arg[1] type     - 注入类型 "before"、"replace" 或 "after"
     * ctx.arg[2] handler  - 回调函数
     *
     * @return 注入 ID
     */
    private static Object inject(FunctionContext<?> ctx) {
        TargetMethod method = parseTarget(Objects.requireNonNull(ctx.getString(0)));
        InjectionType type;
        if (ctx.getArgumentCount() >= 2) {
            type = parseType(ctx.getString(1));
        } else {
            type = InjectionType.BEFORE;
        }
        Function handler = asFunction(ctx.getArgument(ctx.getArgumentCount() - 1));
        InjectionSpec spec = new InjectionSpec(method.className, method.methodName, method.descriptor, type);
        CallbackDispatcher.register(spec.getId(), handler, ctx.getEnvironment());
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

    /**
     * 解析注入类型字符串。
     *
     * @param typeStr 类型字符串 "before"、"replace" 或 "after"
     * @return InjectionType 枚举
     */
    private static InjectionType parseType(String typeStr) {
        if (typeStr == null || "before".equalsIgnoreCase(typeStr)) {
            return InjectionType.BEFORE;
        }
        if ("replace".equalsIgnoreCase(typeStr)) {
            return InjectionType.REPLACE;
        }
        if ("after".equalsIgnoreCase(typeStr)) {
            return InjectionType.AFTER;
        }
        throw new IllegalArgumentException("不支持的注入类型: " + typeStr + "，仅支持 'before'、'replace' 或 'after'");
    }

    /**
     * 将对象转换为 Function。
     *
     * @param obj 待转换对象
     * @return Function 实例
     * @throws IllegalArgumentException 如果对象不是 Function 类型
     */
    private static Function asFunction(Object obj) {
        if (obj instanceof Function) {
            return (Function) obj;
        }
        throw new IllegalArgumentException("回调必须是 Function 类型");
    }

    /**
     * 解析目标方法字符串。
     *
     * @param target 目标字符串，格式为 "className::methodName" 或 "className::methodName(descriptor)"
     * @return TargetMethod 实例
     */
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
    public static final class JvmModule {
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
