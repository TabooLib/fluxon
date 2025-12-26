package org.tabooproject.fluxon.runtime.reflection;

import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.ExportBytecodeGenerator;

import java.lang.invoke.*;

/**
 * 反射访问的 Bootstrap Method
 * 为 invokedynamic 指令提供方法查找和缓存支持
 */
public class ReflectionBootstrap {

    public static final Type TYPE = new Type(ReflectionBootstrap.class);

    // ==================== 方法调用 Bootstrap ====================

    /**
     * Bootstrap Method for invokedynamic (方法调用)
     * JVM 在首次调用时执行此方法，后续调用直接走 CallSite
     *
     * @param lookup     方法查找器
     * @param memberName 成员名（方法名）
     * @param type       方法类型 (Object, Object[])Object
     * @return CallSite 调用点
     */
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String memberName, MethodType type) throws Throwable {
        // 创建 MutableCallSite（支持动态更新 target）
        MutableCallSite callSite = new MutableCallSite(type);
        // 创建查找逻辑的 MethodHandle
        MethodHandle fallback = lookup.findStatic(
                ReflectionBootstrap.class,
                "lookupAndInvoke",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object.class, Object[].class)
        );
        // 绑定 callSite 和 memberName 到 fallback
        MethodHandle target = fallback
                .bindTo(callSite)
                .bindTo(memberName)
                .asType(type);
        callSite.setTarget(target);
        return callSite;
    }

    /**
     * 首次方法调用时的查找逻辑（会被缓存）
     */
    public static Object lookupAndInvoke(MutableCallSite callSite, String memberName, Object target, Object[] args) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot invoke method '" + memberName + "' on null object");
        }
        // 1. 优先检查 ClassBridge（性能最优）
        ClassBridge bridge = ExportBytecodeGenerator.getClassBridge(target.getClass());
        if (bridge != null && bridge.supportsMethod(memberName)) {
            // 创建优化的 MethodHandle 并更新 CallSite
            MethodHandle bridgeHandle = createBridgeHandle(bridge, memberName, callSite.type());
            callSite.setTarget(bridgeHandle);
            return bridgeHandle.invokeWithArguments(target, args);
        }
        // 2. 降级使用 ReflectionCache 查找方法（带缓存）
        Object result = ReflectionHelper.invokeMethod(target, memberName, args);
        // 创建优化的 MethodHandle 并更新 CallSite
        MethodHandle optimizedHandle = createMethodHandle(memberName, callSite.type());
        if (optimizedHandle != null) {
            callSite.setTarget(optimizedHandle);
        }
        return result;
    }

    // ==================== 字段访问 Bootstrap ====================

    /**
     * Bootstrap Method for invokedynamic (字段访问)
     * JVM 在首次调用时执行此方法，后续调用直接走 CallSite
     *
     * @param lookup    方法查找器
     * @param fieldName 字段名
     * @param type      方法类型 (Object)Object
     * @return CallSite 调用点
     */
    public static CallSite bootstrapField(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
        // 创建 MutableCallSite（支持动态更新 target）
        MutableCallSite callSite = new MutableCallSite(type);
        // 创建查找逻辑的 MethodHandle
        MethodHandle fallback = lookup.findStatic(
                ReflectionBootstrap.class,
                "lookupAndGetField",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object.class)
        );
        // 绑定 callSite 和 fieldName 到 fallback
        MethodHandle target = fallback
                .bindTo(callSite)
                .bindTo(fieldName)
                .asType(type);
        callSite.setTarget(target);
        return callSite;
    }

    /**
     * 首次字段访问时的查找逻辑（会被缓存）
     */
    public static Object lookupAndGetField(MutableCallSite callSite, String fieldName, Object target) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot access field '" + fieldName + "' on null object");
        }
        Object result = ReflectionHelper.getField(target, fieldName);
        // 创建优化的 MethodHandle 并更新 CallSite
        MethodHandle optimizedHandle = createFieldHandle(fieldName, callSite.type());
        if (optimizedHandle != null) {
            callSite.setTarget(optimizedHandle);
        }
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 ClassBridge 的 MethodHandle
     */
    private static MethodHandle createBridgeHandle(ClassBridge bridge, String methodName, MethodType callSiteType) throws Throwable {
        // 查找 ClassBridge.invoke(String, Object, Object[]) 方法
        MethodHandle invokeHandle = MethodHandles.publicLookup().findVirtual(
                ClassBridge.class,
                "invoke",
                MethodType.methodType(Object.class, String.class, Object.class, Object[].class)
        );
        // 绑定 bridge 实例和 methodName
        MethodHandle boundHandle = invokeHandle.bindTo(bridge).bindTo(methodName);
        // 调整类型以匹配 CallSite 类型：(Object, Object[])Object
        return boundHandle.asType(callSiteType);
    }

    /**
     * 创建基于 ReflectionCache 的方法调用 MethodHandle
     */
    private static MethodHandle createMethodHandle(String memberName, MethodType callSiteType) {
        try {
            // 查找 ReflectionCache.invokeMethod(Object, String, Object[]) 方法
            MethodHandle invokeHandle = MethodHandles.publicLookup().findStatic(
                    ReflectionHelper.class,
                    "invokeMethod",
                    MethodType.methodType(Object.class, Object.class, String.class, Object[].class)
            );
            // 绑定 memberName 参数
            MethodHandle boundHandle = MethodHandles.insertArguments(invokeHandle, 1, memberName);
            // 调整类型以匹配 CallSite 类型：(Object, Object[])Object
            return boundHandle.asType(callSiteType);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 创建基于 ReflectionCache 的字段访问 MethodHandle
     */
    private static MethodHandle createFieldHandle(String fieldName, MethodType callSiteType) {
        try {
            // 查找 ReflectionCache.getField(Object, String) 方法
            MethodHandle getFieldHandle = MethodHandles.publicLookup().findStatic(
                    ReflectionHelper.class,
                    "getField",
                    MethodType.methodType(Object.class, Object.class, String.class)
            );
            // 绑定 fieldName 参数
            MethodHandle boundHandle = MethodHandles.insertArguments(getFieldHandle, 1, fieldName);
            // 调整类型以匹配 CallSite 类型：(Object)Object
            return boundHandle.asType(callSiteType);
        } catch (Throwable e) {
            return null;
        }
    }
}
