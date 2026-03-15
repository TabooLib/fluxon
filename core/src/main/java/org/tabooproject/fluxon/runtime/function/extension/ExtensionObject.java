package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionObject {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionObject.class);
        // isInstance 返回 Type.Z（boolean），@FluxonFunction 的 int → Type.I 语义不一致，保持手动注册
        runtime.registerExtension(Object.class)
                .function("isInstance", returns(Type.Z).params(Type.OBJECT), context -> {
                    if (context.getTarget() == null) {
                        context.setReturnBool(false);
                        return;
                    }
                    Object arg = context.getRef(0);
                    if (!(arg instanceof Class)) {
                        context.setReturnBool(false);
                        return;
                    }
                    context.setReturnBool(((Class<?>) arg).isInstance(context.getTarget()));
                });
    }

    @FluxonFunction(value = "toString", target = Object.class)
    public static Object extensionToString(Object target) {
        return Objects.toString(target);
    }

    @FluxonFunction(value = "hashCode", target = Object.class)
    public static int extensionHashCode(Object target) {
        return target != null ? target.hashCode() : 0;
    }

    @FluxonFunction(value = "class", target = Object.class)
    public static Object extensionClass(Object target) {
        return target != null ? target.getClass() : null;
    }
}
