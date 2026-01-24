package org.tabooproject.fluxon.inst.function;

import org.tabooproject.fluxon.inst.CallbackDispatcher;
import org.tabooproject.fluxon.inst.InjectionRegistry;
import org.tabooproject.fluxon.inst.InjectionSpec;
import org.tabooproject.fluxon.inst.InjectionType;
import org.tabooproject.fluxon.runtime.*;

import java.util.*;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

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

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("fs:jvm", "jvm", returns(Type.OBJECT).noParams(), ctx -> ctx.setReturnRef(INSTANCE));
        runtime.registerExtension(JvmModule.class, "fs:jvm")
                .function("inject", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), FunctionJvm::inject)
                .function("inject", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT, Type.OBJECT), FunctionJvm::inject)
                .function("restore", returns(Type.Z).params(Type.OBJECT), FunctionJvm::restore)
                .function("injections", returns(Type.OBJECT).noParams(), FunctionJvm::injections);
    }

    /**
     * jvm()::inject(target, type, handler)
     */
    private static void inject(FunctionContext<?> ctx) {
        Object arg0 = ctx.getRef(0);
        TargetMethod method = parseTarget(Objects.requireNonNull(Objects.toString(arg0, null)));
        InjectionType type;
        if (ctx.getArgumentCount() >= 2) {
            Object arg1 = ctx.getRef(1);
            type = parseType(arg1 != null ? arg1.toString() : null);
        } else {
            type = InjectionType.BEFORE;
        }
        Function handler = asFunction(ctx.getRef(ctx.getArgumentCount() - 1));
        InjectionSpec spec = new InjectionSpec(method.className, method.methodName, method.descriptor, type);
        CallbackDispatcher.register(spec.getId(), handler, ctx.getEnvironment());
        ctx.setReturnRef(InjectionRegistry.getInstance().register(spec));
    }

    /**
     * jvm()::restore(idOrTarget)
     */
    private static void restore(FunctionContext<?> ctx) {
        Object arg0 = ctx.getRef(0);
        String idOrTarget = arg0 != null ? arg0.toString() : null;
        if (InjectionRegistry.getInstance().unregister(idOrTarget)) {
            CallbackDispatcher.unregister(idOrTarget);
            ctx.setReturnBool(true);
            return;
        }
        if (Objects.requireNonNull(idOrTarget).contains("::")) {
            TargetMethod method = parseTarget(idOrTarget);
            ctx.setReturnBool(InjectionRegistry.getInstance().unregisterByTarget(method.className, method.methodName, method.descriptor));
            return;
        }
        ctx.setReturnBool(false);
    }

    /**
     * jvm()::injections()
     */
    private static void injections(FunctionContext<?> ctx) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (InjectionSpec spec : InjectionRegistry.getInstance().getAllSpecs()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", spec.getId());
            item.put("target", spec.getTarget());
            item.put("type", spec.getType().name().toLowerCase());
            result.add(item);
        }
        ctx.setReturnRef(result);
    }

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

    public static final class JvmModule {
    }

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
