package org.tabooproject.fluxon.runtime.reflection.bootstrap;

import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.ExportBytecodeGenerator;
import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
import org.tabooproject.fluxon.runtime.reflection.resolve.MethodResolver;
import org.tabooproject.fluxon.runtime.reflection.util.PolymorphicInlineCache;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;

import java.lang.invoke.*;

/**
 * 方法调用的 Bootstrap Method
 * 为 invokedynamic 指令提供方法查找和 PIC 缓存支持
 */
public class MethodBootstrap {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    /**
     * Bootstrap Method for invokedynamic (方法调用)
     * JVM 在首次调用时执行此方法，后续调用直接走 CallSite
     */
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String memberName, MethodType type) throws Throwable {
        MutableCallSite callSite = new MutableCallSite(type);
        MethodHandle fallback = lookup.findStatic(
                MethodBootstrap.class,
                "lookupAndInvoke",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object.class, Object[].class)
        );
        MethodHandle target = fallback
                .bindTo(callSite)
                .bindTo(memberName)
                .asType(type);
        callSite.setTarget(target);
        return callSite;
    }

    /**
     * 首次方法调用时的查找逻辑（PIC 实现）
     * 每次类型不匹配时会添加新的缓存条目到链头
     */
    public static Object lookupAndInvoke(MutableCallSite callSite, String memberName, Object target, Object[] args) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot invoke method '" + memberName + "' on null object");
        }
        Class<?> targetClass = target.getClass();
        // 提取参数类型签名
        Class<?>[] argTypes = TypeCompatibility.extractArgTypes(args);
        // 1. 优先检查 ClassBridge（性能最优，且无多态问题）
        ClassBridge bridge = ExportBytecodeGenerator.getClassBridge(targetClass);
        if (bridge != null && bridge.supportsMethod(memberName)) {
            MethodHandle bridgeHandle = createBridgeHandle(bridge, memberName, callSite.type());
            callSite.setTarget(bridgeHandle);
            return bridgeHandle.invokeWithArguments(target, args);
        }
        // 2. 尝试创建类型特化的直接方法调用
        MethodHandle specialized = MethodResolver.tryCreateSpecializedMethodHandle(targetClass, memberName, args, callSite.type());
        if (specialized != null) {
            if (PolymorphicInlineCache.canAddEntry(callSite)) {
                // 在同步块内获取当前 target，确保链的一致性
                MethodHandle currentTarget = callSite.getTarget();
                MethodHandle guarded = PolymorphicInlineCache.createMethodEntry(targetClass, argTypes, specialized, currentTarget, callSite.type());
                callSite.setTarget(guarded);
                // 同步点：确保其他线程能看到更新
                MutableCallSite.syncAll(new MutableCallSite[]{callSite});
                PolymorphicInlineCache.incrementDepth(callSite);
            }
            return specialized.invokeWithArguments(target, args);
        }
        // 3. 降级使用 MethodResolver（不缓存或已达深度限制）
        return ReflectionHelper.invokeMethod(target, memberName, args);
    }

    /**
     * 创建 ClassBridge 的 MethodHandle
     */
    private static MethodHandle createBridgeHandle(ClassBridge bridge, String methodName, MethodType callSiteType) throws Throwable {
        MethodHandle invokeHandle = LOOKUP.findVirtual(
                ClassBridge.class,
                "invoke",
                MethodType.methodType(Object.class, String.class, Object.class, Object[].class)
        );
        MethodHandle boundHandle = invokeHandle.bindTo(bridge).bindTo(methodName);
        return boundHandle.asType(callSiteType);
    }
}
