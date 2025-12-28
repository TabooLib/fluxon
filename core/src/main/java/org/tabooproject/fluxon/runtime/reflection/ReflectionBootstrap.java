package org.tabooproject.fluxon.runtime.reflection;

import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.reflection.bootstrap.ConstructorBootstrap;
import org.tabooproject.fluxon.runtime.reflection.bootstrap.FieldBootstrap;
import org.tabooproject.fluxon.runtime.reflection.bootstrap.MethodBootstrap;
import org.tabooproject.fluxon.runtime.reflection.bootstrap.StaticMethodBootstrap;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * 反射访问的 Bootstrap Method 门面类
 * 为 invokedynamic 指令提供方法查找和缓存支持
 * <p>
 * 实际实现委托给具体的 Bootstrap 类：
 * - {@link MethodBootstrap} 处理方法调用
 * - {@link FieldBootstrap} 处理字段访问
 * - {@link ConstructorBootstrap} 处理构造函数调用
 */
public class ReflectionBootstrap {

    public static final Type TYPE = new Type(ReflectionBootstrap.class);

    /**
     * Bootstrap Method for invokedynamic (方法调用)
     * JVM 在首次调用时执行此方法，后续调用直接走 CallSite
     */
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String memberName, MethodType type) throws Throwable {
        return MethodBootstrap.bootstrap(lookup, memberName, type);
    }

    /**
     * Bootstrap Method for invokedynamic (字段访问)
     */
    public static CallSite bootstrapField(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
        return FieldBootstrap.bootstrap(lookup, fieldName, type);
    }

    /**
     * Bootstrap Method for invokedynamic (构造函数调用)
     * 用于处理 new ClassName(args) 语法的字节码编译
     */
    public static CallSite bootstrapConstructor(MethodHandles.Lookup lookup, String name, MethodType type) throws Throwable {
        return ConstructorBootstrap.bootstrap(lookup, name, type);
    }

    /**
     * Bootstrap Method for invokedynamic (静态方法调用)
     * 签名: (Class, Object[]) -> Object
     */
    public static CallSite bootstrapStaticMethod(MethodHandles.Lookup lookup, String memberName, MethodType type) throws Throwable {
        return StaticMethodBootstrap.bootstrapMethod(lookup, memberName, type);
    }

    /**
     * Bootstrap Method for invokedynamic (静态字段访问)
     * 签名: (Class) -> Object
     */
    public static CallSite bootstrapStaticField(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
        return StaticMethodBootstrap.bootstrapField(lookup, fieldName, type);
    }
}
