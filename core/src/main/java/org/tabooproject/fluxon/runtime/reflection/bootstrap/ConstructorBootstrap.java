package org.tabooproject.fluxon.runtime.reflection.bootstrap;

import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
import org.tabooproject.fluxon.runtime.reflection.resolve.ConstructorResolver;
import org.tabooproject.fluxon.runtime.reflection.util.PolymorphicInlineCache;
import org.tabooproject.fluxon.runtime.reflection.util.TypeCompatibility;

import java.lang.invoke.*;

/**
 * 构造函数调用的 Bootstrap Method
 * 为 invokedynamic 指令提供构造函数查找和 PIC 缓存支持
 */
public class ConstructorBootstrap {

    /**
     * Bootstrap Method for invokedynamic (构造函数调用)
     * 用于处理 new ClassName(args) 语法的字节码编译
     */
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type) throws Throwable {
        MutableCallSite callSite = new MutableCallSite(type);
        MethodHandle fallback = lookup.findStatic(
                ConstructorBootstrap.class,
                "lookupAndConstruct",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object[].class)
        );
        MethodHandle target = fallback
                .bindTo(callSite)
                .asType(type);
        callSite.setTarget(target);
        return callSite;
    }

    /**
     * 首次构造函数调用时的查找逻辑（PIC 实现）
     * @param callSite 可变调用站点，用于缓存优化后的 MethodHandle
     * @param className 要构造的类的完全限定名
     * @param args 构造函数参数
     * @return 新创建的对象实例
     */
    public static Object lookupAndConstruct(MutableCallSite callSite, String className, Object[] args) throws Throwable {
        // 1. 加载类
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
        // 提取参数类型签名
        Class<?>[] argTypes = TypeCompatibility.extractArgTypes(args);
        // 2. 调用 ConstructorResolver 执行构造
        Object result = ReflectionHelper.invokeConstructor(clazz, args);
        // 3. 尝试创建类型特化的构造函数 MethodHandle 用于后续调用
        MethodHandle specialized = ConstructorResolver.tryCreateSpecializedConstructorHandle(clazz, args, callSite.type());
        if (specialized != null) {
            if (PolymorphicInlineCache.canAddEntry(callSite)) {
                MethodHandle currentTarget = callSite.getTarget();
                // 为特定类名 + 参数类型创建 PIC entry
                MethodHandle guard = PolymorphicInlineCache.createConstructorEntry(className, argTypes, specialized, currentTarget, callSite.type());
                callSite.setTarget(guard);
                PolymorphicInlineCache.incrementDepth(callSite);
            }
        }
        return result;
    }
}
