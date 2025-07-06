package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Objects;

public class ExtensionObject {

    public static void init(FluxonRuntime runtime) {
        // 转换为字符串
        runtime.registerExtensionFunction(Object.class, "toString", 0, (target, args) -> Objects.toString(target));
        runtime.registerExtensionFunction(Object.class, "hashCode", 0, (target, args) -> target != null ? target.hashCode() : 0);
        // 获取对象的类
        runtime.registerExtensionFunction(Object.class, "class", 0, (target, args) -> target != null ? target.getClass() : null);
        // 检查对象是否是指定类的实例
        runtime.registerExtensionFunction(Object.class, "isInstance", 1, (target, args) -> {
            if (target == null) {
                return false;
            }
            Class<?> clazz = (Class<?>) args[0];
            return clazz.isInstance(target);
        });
    }
}
