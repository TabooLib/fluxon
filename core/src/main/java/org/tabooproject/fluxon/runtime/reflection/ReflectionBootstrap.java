package org.tabooproject.fluxon.runtime.reflection;

import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.ExportBytecodeGenerator;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射访问的 Bootstrap Method
 * 为 invokedynamic 指令提供方法查找和缓存支持
 */
public class ReflectionBootstrap {

    public static final Type TYPE = new Type(ReflectionBootstrap.class);
    
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    
    /**
     * PIC 最大缓存深度
     * 超过此深度后不再添加新的缓存条目，始终走 fallback
     */
    static final int MAX_PIC_DEPTH = 8;
    
    /**
     * 每个 CallSite 的 PIC 深度计数器
     * 使用 ConcurrentHashMap 确保线程安全
     */
    private static final ConcurrentHashMap<MutableCallSite, Integer> PIC_DEPTH_MAP = new ConcurrentHashMap<>();
    
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
     * 首次方法调用时的查找逻辑（PIC 实现）
     * 每次类型不匹配时会添加新的缓存条目到链头
     */
    public static Object lookupAndInvoke(MutableCallSite callSite, String memberName, Object target, Object[] args) throws Throwable {
        if (target == null) {
            throw new NullPointerException("Cannot invoke method '" + memberName + "' on null object");
        }
        Class<?> targetClass = target.getClass();
        
        // 提取参数类型签名
        Class<?>[] argTypes = extractArgTypes(args);
        
        // 1. 优先检查 ClassBridge（性能最优，且无多态问题）
        ClassBridge bridge = ExportBytecodeGenerator.getClassBridge(targetClass);
        if (bridge != null && bridge.supportsMethod(memberName)) {
            MethodHandle bridgeHandle = createBridgeHandle(bridge, memberName, callSite.type());
            callSite.setTarget(bridgeHandle);
            return bridgeHandle.invokeWithArguments(target, args);
        }
        
        // 2. 尝试创建类型特化的直接方法调用
        MethodHandle specialized = tryCreateSpecializedMethodHandle(targetClass, memberName, args, callSite.type());
        
        if (specialized != null) {
            // 原子性更新：整个读取-创建-更新操作必须在同步块内
            synchronized (callSite) {
                int currentDepth = PIC_DEPTH_MAP.getOrDefault(callSite, 0);
                if (currentDepth < MAX_PIC_DEPTH) {
                    // 在同步块内获取当前 target，确保链的一致性
                    MethodHandle currentTarget = callSite.getTarget();
                    MethodHandle guarded = createPICMethodEntry(
                        targetClass, argTypes, specialized,
                        currentTarget,  // fallback 到当前链
                        callSite.type()
                    );
                    callSite.setTarget(guarded);
                    // 同步点：确保其他线程能看到更新
                    MutableCallSite.syncAll(new MutableCallSite[] { callSite });
                    PIC_DEPTH_MAP.put(callSite, currentDepth + 1);
                }
            }
            
            return specialized.invokeWithArguments(target, args);
        }
        
        // 3. 降级使用 ReflectionHelper（不缓存或已达深度限制）
        return ReflectionHelper.invokeMethod(target, memberName, args);
    }
    
    /**
     * 提取参数类型数组
     */
    private static Class<?>[] extractArgTypes(Object[] args) {
        Class<?>[] argTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i] != null ? args[i].getClass() : Void.class; // 用 Void.class 表示 null
        }
        return argTypes;
    }
    
    /**
     * 创建 PIC 方法调用条目
     * 包含 receiver 类型检查和所有参数类型检查
     */
    private static MethodHandle createPICMethodEntry(
            Class<?> targetClass, 
            Class<?>[] argTypes, 
            MethodHandle specialized, 
            MethodHandle fallback, 
            MethodType callSiteType) {
        try {
            // 创建组合 guard：检查 receiver 类型 + 所有参数类型
            MethodHandle guard = createFullTypeGuard(targetClass, argTypes, callSiteType);
            
            MethodHandle adapted = specialized.asType(callSiteType);
            MethodHandle adaptedFallback = fallback.asType(callSiteType);
            
            return MethodHandles.guardWithTest(guard, adapted, adaptedFallback);
        } catch (Throwable e) {
            return fallback;
        }
    }
    
    /**
     * 创建完整类型检查 guard
     * 检查 receiver 类型和所有参数类型
     * 签名: (Object, Object[]) -> boolean
     */
    private static MethodHandle createFullTypeGuard(Class<?> targetClass, Class<?>[] argTypes, MethodType callSiteType) throws Throwable {
        // 创建检查方法的 MethodHandle
        MethodHandle checker = LOOKUP.findStatic(
            ReflectionBootstrap.class,
            "checkFullTypeSignature",
            MethodType.methodType(boolean.class, Class.class, Class[].class, Object.class, Object[].class)
        );
        
        // 绑定期望的类型签名
        MethodHandle bound = MethodHandles.insertArguments(checker, 0, targetClass, argTypes);
        
        // 返回签名为 (Object, Object[]) -> boolean 的 guard
        return bound;
    }
    
    /**
     * 检查完整类型签名（receiver + 所有参数）
     * 用于 PIC guard
     */
    public static boolean checkFullTypeSignature(Class<?> expectedTarget, Class<?>[] expectedArgs, Object target, Object[] args) {
        // 检查 receiver 类型
        if (target == null || target.getClass() != expectedTarget) {
            return false;
        }
        
        // 检查参数数量
        if (args.length != expectedArgs.length) {
            return false;
        }
        
        // 检查每个参数类型
        for (int i = 0; i < args.length; i++) {
            Class<?> expectedArg = expectedArgs[i];
            Object actualArg = args[i];
            if (expectedArg == Void.class) {
                // 期望 null
                if (actualArg != null) return false;
            } else {
                // 期望特定类型 - 精确匹配
                if (actualArg == null) return false;
                Class<?> actualClass = actualArg.getClass();
                if (actualClass != expectedArg) {
                    return false;
                }
            }
        }
        
        return true;
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
        // 3. 降级使用 ReflectionHelper（不缓存，每次动态查找）
        return ReflectionHelper.getField(target, fieldName);
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
            // 确保方法可访问（处理嵌套类等情况）
            method.setAccessible(true);
            MethodHandle mh = MethodHandles.lookup().unreflect(method);
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
            if (isParametersCompatible(method.getParameterTypes(), argTypes)) {
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
            return param == char.class && arg == Character.class;
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

    // ==================== 构造函数调用 Bootstrap ====================

    /**
     * Bootstrap Method for invokedynamic (构造函数调用)
     * 用于处理 new ClassName(args) 语法的字节码编译
     */
    public static CallSite bootstrapConstructor(MethodHandles.Lookup lookup, String name, MethodType type) throws Throwable {
        MutableCallSite callSite = new MutableCallSite(type);
        MethodHandle fallback = lookup.findStatic(
                ReflectionBootstrap.class,
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
        Class<?>[] argTypes = extractArgTypes(args);
        
        // 2. 调用 ReflectionHelper 执行构造
        Object result = ReflectionHelper.invokeConstructor(clazz, args);
        
        // 3. 尝试创建类型特化的构造函数 MethodHandle 用于后续调用
        MethodHandle specialized = tryCreateSpecializedConstructorHandle(clazz, args, callSite.type());
        if (specialized != null) {
            // 原子性更新：整个读取-创建-更新操作必须在同步块内
            synchronized (callSite) {
                int currentDepth = PIC_DEPTH_MAP.getOrDefault(callSite, 0);
                if (currentDepth < MAX_PIC_DEPTH) {
                    MethodHandle currentTarget = callSite.getTarget();
                    
                    // 为特定类名 + 参数类型创建 PIC entry
                    MethodHandle guard = createPICConstructorEntry(className, argTypes, specialized, currentTarget, callSite.type());
                    callSite.setTarget(guard);
                    PIC_DEPTH_MAP.put(callSite, currentDepth + 1);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 创建 PIC 构造函数调用条目
     * 包含类名检查和所有参数类型检查
     */
    private static MethodHandle createPICConstructorEntry(
            String className,
            Class<?>[] argTypes,
            MethodHandle specialized,
            MethodHandle fallback,
            MethodType callSiteType) {
        try {
            // 创建组合 guard：检查类名 + 所有参数类型
            MethodHandle guard = createConstructorTypeGuard(className, argTypes);
            
            MethodHandle adapted = specialized.asType(callSiteType);
            MethodHandle adaptedFallback = fallback.asType(callSiteType);
            
            return MethodHandles.guardWithTest(guard, adapted, adaptedFallback);
        } catch (Throwable e) {
            return fallback;
        }
    }
    
    /**
     * 创建构造函数类型检查 guard
     * 签名: (String, Object[]) -> boolean
     */
    private static MethodHandle createConstructorTypeGuard(String className, Class<?>[] argTypes) throws Throwable {
        MethodHandle checker = LOOKUP.findStatic(
            ReflectionBootstrap.class,
            "checkConstructorTypeSignature",
            MethodType.methodType(boolean.class, String.class, Class[].class, String.class, Object[].class)
        );
        
        // 绑定期望的类型签名
        return MethodHandles.insertArguments(checker, 0, className, argTypes);
    }
    
    /**
     * 检查构造函数类型签名（类名 + 所有参数）
     * 用于 PIC guard
     */
    public static boolean checkConstructorTypeSignature(String expectedClassName, Class<?>[] expectedArgs, String className, Object[] args) {
        // 检查类名
        if (!expectedClassName.equals(className)) {
            return false;
        }
        
        // 检查参数数量
        if (args.length != expectedArgs.length) {
            return false;
        }
        
        // 检查每个参数类型
        for (int i = 0; i < args.length; i++) {
            Class<?> expectedArg = expectedArgs[i];
            if (expectedArg == Void.class) {
                if (args[i] != null) return false;
            } else {
                if (args[i] == null || args[i].getClass() != expectedArg) return false;
            }
        }
        
        return true;
    }

    /**
     * 尝试创建直接构造函数调用的 MethodHandle
     */
    private static MethodHandle tryCreateSpecializedConstructorHandle(Class<?> clazz, Object[] args, MethodType callSiteType) {
        try {
            // 根据参数类型查找构造函数
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            // 尝试找到匹配的构造函数
            Constructor<?> constructor = findBestConstructor(clazz, argTypes);
            if (constructor == null) {
                return null;
            }
            MethodHandle mh = LOOKUP.unreflectConstructor(constructor);
            // 转换为接受 Object[] 参数的形式
            mh = mh.asSpreader(Object[].class, args.length);
            // 添加一个被忽略的 className 参数使签名变为 (String, Object[])Object
            mh = MethodHandles.dropArguments(mh, 0, String.class);
            return mh.asType(callSiteType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查找最佳匹配的构造函数
     */
    private static Constructor<?> findBestConstructor(Class<?> clazz, Class<?>[] argTypes) {
        // 首先尝试精确匹配
        try {
            return clazz.getConstructor(argTypes);
        } catch (NoSuchMethodException ignored) {
        }
        // 尝试找到兼容的构造函数
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (isParametersCompatible(constructor.getParameterTypes(), argTypes)) {
                return constructor;
            }
        }
        return null;
    }

    /**
     * 检查参数类型是否兼容
     *
     * @param paramTypes 方法/构造函数的参数类型
     * @param argTypes   实际参数类型
     * @return 是否兼容
     */
    private static boolean isParametersCompatible(Class<?>[] paramTypes, Class<?>[] argTypes) {
        if (paramTypes.length != argTypes.length) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (argTypes[i] != null && !isAssignableFrom(paramTypes[i], argTypes[i])) {
                return false;
            }
        }
        return true;
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

    // ==================== 测试支持方法 ====================
    
    /**
     * 获取指定 CallSite 的 PIC 深度（用于测试）
     */
    public static int getPICDepth(MutableCallSite callSite) {
        return PIC_DEPTH_MAP.getOrDefault(callSite, 0);
    }
    
    /**
     * 清除所有 PIC 缓存（用于测试）
     */
    public static void clearPICCache() {
        PIC_DEPTH_MAP.clear();
    }
    
    /**
     * 获取 PIC 缓存大小（用于测试）
     */
    public static int getPICCacheSize() {
        return PIC_DEPTH_MAP.size();
    }
}
