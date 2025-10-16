package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;

import java.util.Objects;

public class ExtensionObject {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Object.class)
                // 转换为字符串
                .function("toString", 0, (context) -> Objects.toString(context.getTarget()))
                .function("hashCode", 0, (context) -> context.getTarget() != null ? context.getTarget().hashCode() : 0)
                // 获取对象的类
                .function("class", 0, (context) -> context.getTarget() != null ? context.getTarget().getClass() : null)
                // 检查对象是否是指定类的实例
                .function("isInstance", 1, (context) -> {
                    if (context.getTarget() == null) {
                        return false;
                    }
                    Class<?> clazz = context.getArgumentByType(0, Class.class);
                    if (clazz == null) {
                        return false;
                    }
                    return clazz.isInstance(context.getTarget());
                });
    }
}
