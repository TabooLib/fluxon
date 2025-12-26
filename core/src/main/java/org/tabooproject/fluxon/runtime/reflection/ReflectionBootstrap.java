package org.tabooproject.fluxon.runtime.reflection;

import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.ExportBytecodeGenerator;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射访问的 Bootstrap Method
 * 为 invokedynamic 指令提供方法查找和缓存支持
 * <p>
 * 优化策略：
 * 1. 类型特化：首次调用时根据实际类型创建直接的字段/方法访问 MethodHandle
 * 2. 类型保护：使用 guardWithTest 在类型变化时回退到通用路径
 * 3. ClassBridge 优先：如果存在预生成的桥接类，优先使用
 */
public class ReflectionBootstrap {

    public static final Type TYPE = new Type(ReflectionBootstrap.class);
    
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    
    // 类型检查的 MethodHandle: boolean isInstance(Class<?>, Object)
    private static final MethodHandle IS_INSTANCE;
    
    static {
        try {
            IS_INSTANCE = LOOKUP.findVirtual(Class.class, "isInstance", MethodType.methodType(boolean.class, Object.class));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ==================== 方法调用 Bootstrap ====================

    /**
     * Bootstrap Method for invokedynamic (方法调用)
     * JVM 在首次调用时执行此方法，后续调用直接走 CallSite
     */
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String memberName, MethodType type) throws Throwable {
        MutableCallSite callSite = new MutableCallSite(type);
        MethodHandle fallback = lookup.findStatic(
                ReflectionBootstrap.class,
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
     * 首次方法调用时的查找逻辑（会被缓存）
     * 优化：尝试创建类型特化的直接方法调用
     */
    public static Object lookupAndInvoke(MutableCallSite callSite, String memberName, Object target, Object[] args) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot invoke method '" + memberName + "' on null object");
        }
        Class<?> targetClass = target.getClass();
        // 1. 优先检查 ClassBridge（性能最优）
        ClassBridge bridge = ExportBytecodeGenerator.getClassBridge(targetClass);
        if (bridge != null && bridge.supportsMethod(memberName)) {
            MethodHandle bridgeHandle = createBridgeHandle(bridge, memberName, callSite.type());
            callSite.setTarget(bridgeHandle);
            return bridgeHandle.invokeWithArguments(target, args);
        }
        // 2. 尝试创建类型特化的直接方法调用
        MethodHandle specialized = tryCreateSpecializedMethodHandle(targetClass, memberName, args, callSite.type());
        if (specialized != null) {
            // 创建带类型保护的 MethodHandle
            MethodHandle guarded = createGuardedMethodHandle(
                targetClass, specialized, 
                createMethodFallback(callSite, memberName), 
                callSite.type()
            );
            callSite.setTarget(guarded);
            return specialized.invokeWithArguments(target, args);
        }
        // 3. 降级使用 ReflectionHelper
        Object result = ReflectionHelper.invokeMethod(target, memberName, args);
        MethodHandle fallbackHandle = createMethodHandle(memberName, callSite.type());
        if (fallbackHandle != null) {
            callSite.setTarget(fallbackHandle);
        }
        return result;
    }

    // ==================== 字段访问 Bootstrap ====================

    /**
     * Bootstrap Method for invokedynamic (字段访问)
     */
    public static CallSite bootstrapField(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
        MutableCallSite callSite = new MutableCallSite(type);
        MethodHandle fallback = lookup.findStatic(
                ReflectionBootstrap.class,
                "lookupAndGetField",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object.class)
        );
        MethodHandle target = fallback
                .bindTo(callSite)
                .bindTo(fieldName)
                .asType(type);
        callSite.setTarget(target);
        return callSite;
    }

    /**
     * 首次字段访问时的查找逻辑
     * 优化：尝试创建类型特化的直接字段访问
     */
    public static Object lookupAndGetField(MutableCallSite callSite, String fieldName, Object target) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot access field '" + fieldName + "' on null object");
        }
        Class<?> targetClass = target.getClass();
        // 1. 尝试创建类型特化的直接字段访问
        MethodHandle specialized = tryCreateSpecializedFieldHandle(targetClass, fieldName);
        if (specialized != null) {
            // 创建带类型保护的 MethodHandle
            MethodHandle guarded = createGuardedFieldHandle(
                targetClass, specialized,
                createFieldFallback(callSite, fieldName),
                callSite.type()
            );
            callSite.setTarget(guarded);
            return specialized.invoke(target);
        }
        // 2. 尝试 getter 方法
        MethodHandle getter = tryCreateGetterHandle(targetClass, fieldName);
        if (getter != null) {
            MethodHandle guarded = createGuardedFieldHandle(
                targetClass, getter,
                createFieldFallback(callSite, fieldName),
                callSite.type()
            );
            callSite.setTarget(guarded);
            return getter.invoke(target);
        }
        // 3. 降级使用 ReflectionHelper
        Object result = ReflectionHelper.getField(target, fieldName);
        MethodHandle fallbackHandle = createFieldHandle(fieldName, callSite.type());
        if (fallbackHandle != null) {
            callSite.setTarget(fallbackHandle);
        }
        return result;
    }

    // ==================== 类型特化辅助方法 ====================
    
    /**
     * 尝试创建直接字段访问的 MethodHandle
     */
    private static MethodHandle tryCreateSpecializedFieldHandle(Class<?> targetClass, String fieldName) {
        try {
            Field field = targetClass.getField(fieldName);
            MethodHandle mh = LOOKUP.unreflectGetter(field);
            // 处理静态字段（无接收者参数）
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                // 静态字段：添加一个被忽略的参数，使签名变为 (Object)Object
                mh = MethodHandles.dropArguments(mh, 0, Object.class);
            }
            // 适配为 (Object)Object 签名
            return mh.asType(MethodType.methodType(Object.class, Object.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
    
    /**
     * 尝试创建 getter 方法的 MethodHandle
     */
    private static MethodHandle tryCreateGetterHandle(Class<?> targetClass, String fieldName) {
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String[] patterns = {"get" + capitalized, fieldName, "is" + capitalized};
        for (String methodName : patterns) {
            try {
                Method method = targetClass.getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    MethodHandle mh = LOOKUP.unreflect(method);
                    // 适配为 (Object)Object 签名
                    return mh.asType(MethodType.methodType(Object.class, Object.class));
                }
            } catch (NoSuchMethodException | IllegalAccessException ignored) {
            }
        }
        return null;
    }
    
    /**
     * 尝试创建直接方法调用的 MethodHandle
     */
    private static MethodHandle tryCreateSpecializedMethodHandle(Class<?> targetClass, String methodName, Object[] args, MethodType callSiteType) {
        try {
            // 根据参数类型查找方法
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            // 尝试精确匹配
            Method method = findBestMethod(targetClass, methodName, argTypes);
            if (method == null) {
                return null;
            }
            MethodHandle mh = LOOKUP.unreflect(method);
            // 转换为接受 Object[] 参数的形式
            mh = mh.asSpreader(Object[].class, args.length);
            return mh.asType(callSiteType);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 查找最佳匹配的方法
     */
    private static Method findBestMethod(Class<?> targetClass, String methodName, Class<?>[] argTypes) {
        // 首先尝试精确匹配
        try {
            return targetClass.getMethod(methodName, argTypes);
        } catch (NoSuchMethodException ignored) {
        }
        // 尝试找到兼容的方法
        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (method.getParameterCount() != argTypes.length) continue;
            Class<?>[] paramTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int i = 0; i < paramTypes.length; i++) {
                if (argTypes[i] != null && !isAssignableFrom(paramTypes[i], argTypes[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }
    
    /**
     * 检查类型是否兼容（考虑自动装箱）
     */
    private static boolean isAssignableFrom(Class<?> param, Class<?> arg) {
        if (param.isAssignableFrom(arg)) return true;
        // 处理原始类型装箱
        if (param.isPrimitive()) {
            if (param == int.class && arg == Integer.class) return true;
            if (param == long.class && arg == Long.class) return true;
            if (param == double.class && arg == Double.class) return true;
            if (param == float.class && arg == Float.class) return true;
            if (param == boolean.class && arg == Boolean.class) return true;
            if (param == byte.class && arg == Byte.class) return true;
            if (param == short.class && arg == Short.class) return true;
            if (param == char.class && arg == Character.class) return true;
        }
        return false;
    }
    
    /**
     * 创建带类型保护的字段访问 MethodHandle
     */
    private static MethodHandle createGuardedFieldHandle(Class<?> targetClass, MethodHandle specialized, MethodHandle fallback, MethodType callSiteType) {
        try {
            // 创建类型检查: target instanceof TargetClass
            MethodHandle typeCheck = IS_INSTANCE.bindTo(targetClass);
            // 适配 specialized 的返回类型（可能是原始类型，需要装箱）
            MethodHandle adapted = specialized;
            if (adapted.type().returnType() != callSiteType.returnType()) {
                adapted = adapted.asType(adapted.type().changeReturnType(callSiteType.returnType()));
            }
            // 适配参数类型
            if (adapted.type().parameterType(0) != callSiteType.parameterType(0)) {
                adapted = adapted.asType(callSiteType);
            }
            MethodHandle adaptedFallback = fallback.asType(callSiteType);
            return MethodHandles.guardWithTest(typeCheck, adapted, adaptedFallback);
        } catch (Exception e) {
            return fallback.asType(callSiteType);
        }
    }
    
    /**
     * 创建带类型保护的方法调用 MethodHandle
     */
    private static MethodHandle createGuardedMethodHandle(Class<?> targetClass, MethodHandle specialized, MethodHandle fallback, MethodType callSiteType) {
        try {
            // 创建类型检查（只检查第一个参数 target）
            MethodHandle typeCheck = IS_INSTANCE.bindTo(targetClass);
            // 丢弃额外的参数（args 数组）
            typeCheck = MethodHandles.dropArguments(typeCheck, 1, Object[].class);
            MethodHandle adapted = specialized.asType(callSiteType);
            MethodHandle adaptedFallback = fallback.asType(callSiteType);
            return MethodHandles.guardWithTest(typeCheck, adapted, adaptedFallback);
        } catch (Exception e) {
            return fallback;
        }
    }
    
    /**
     * 创建字段访问的 fallback MethodHandle
     */
    private static MethodHandle createFieldFallback(MutableCallSite callSite, String fieldName) {
        try {
            MethodHandle fallback = LOOKUP.findStatic(
                ReflectionBootstrap.class,
                "lookupAndGetField",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object.class)
            );
            return fallback.bindTo(callSite).bindTo(fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 创建方法调用的 fallback MethodHandle
     */
    private static MethodHandle createMethodFallback(MutableCallSite callSite, String memberName) {
        try {
            MethodHandle fallback = LOOKUP.findStatic(
                ReflectionBootstrap.class,
                "lookupAndInvoke",
                MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object.class, Object[].class)
            );
            return fallback.bindTo(callSite).bindTo(memberName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 原有辅助方法 ====================

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

    /**
     * 创建基于 ReflectionHelper 的方法调用 MethodHandle（降级路径）
     */
    private static MethodHandle createMethodHandle(String memberName, MethodType callSiteType) {
        try {
            MethodHandle invokeHandle = LOOKUP.findStatic(
                    ReflectionHelper.class,
                    "invokeMethod",
                    MethodType.methodType(Object.class, Object.class, String.class, Object[].class)
            );
            MethodHandle boundHandle = MethodHandles.insertArguments(invokeHandle, 1, memberName);
            return boundHandle.asType(callSiteType);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 创建基于 ReflectionHelper 的字段访问 MethodHandle（降级路径）
     */
    private static MethodHandle createFieldHandle(String fieldName, MethodType callSiteType) {
        try {
            MethodHandle getFieldHandle = LOOKUP.findStatic(
                    ReflectionHelper.class,
                    "getField",
                    MethodType.methodType(Object.class, Object.class, String.class)
            );
            MethodHandle boundHandle = MethodHandles.insertArguments(getFieldHandle, 1, fieldName);
            return boundHandle.asType(callSiteType);
        } catch (Throwable e) {
            return null;
        }
    }
}
