package org.tabooproject.fluxon.runtime.stdlib;

import sun.misc.Unsafe;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Unsafe 访问工具类
 * 提供对 Unsafe 和 MethodHandles.Lookup 的安全访问
 */
@SuppressWarnings("unchecked")
public final class UnsafeAccess {

    private static final Unsafe unsafe;
    private static final MethodHandles.Lookup lookup;

    static {
        try {
            // 获取 Unsafe 实例
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            unsafe = (Unsafe) theUnsafeField.get(null);

            // 确保 MethodHandles.Lookup 类初始化
            try {
                Method ensureClassInitialized = Unsafe.class.getDeclaredMethod("ensureClassInitialized", Class.class);
                ensureClassInitialized.invoke(unsafe, MethodHandles.Lookup.class);
            } catch (Throwable ignored) {
                // 修复 JDK22 兼容性
                // MethodHandles.lookup().ensureInitialized(MethodHandles.Lookup.class)
                try {
                    Method ensureInitialized = MethodHandles.Lookup.class.getDeclaredMethod("ensureInitialized", Class.class);
                    ensureInitialized.invoke(MethodHandles.lookup(), MethodHandles.Lookup.class);
                } catch (Throwable alsoIgnored) {
                    // 如果都失败了，继续执行，可能在某些 JDK 版本中不需要显式初始化
                }
            }

            // 获取 IMPL_LOOKUP 字段
            Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object lookupBase = unsafe.staticFieldBase(lookupField);
            long lookupOffset = unsafe.staticFieldOffset(lookupField);
            lookup = (MethodHandles.Lookup) unsafe.getObject(lookupBase, lookupOffset);

        } catch (Throwable t) {
            throw new IllegalStateException("Unsafe not found", t);
        }
    }

    /**
     * 私有构造函数，防止实例化
     */
    private UnsafeAccess() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取 Unsafe 实例
     *
     * @return Unsafe 实例
     */
    public static Unsafe getUnsafe() {
        return unsafe;
    }

    /**
     * 获取 MethodHandles.Lookup 实例
     *
     * @return MethodHandles.Lookup 实例
     */
    public static MethodHandles.Lookup getLookup() {
        return lookup;
    }

    /**
     * 设置字段值
     *
     * @param src   源对象，如果是静态字段则为 null
     * @param field 要设置的字段
     * @param value 要设置的值
     * @throws Throwable 如果设置失败
     */
    public static void put(Object src, Field field, Object value) throws Throwable {
        MethodHandle methodHandle = lookup.unreflectSetter(field);
        if (Modifier.isStatic(field.getModifiers())) {
            methodHandle.invokeWithArguments(value);
        } else {
            methodHandle.bindTo(src).invokeWithArguments(value);
        }
    }

    /**
     * 获取字段值
     *
     * @param src   源对象，如果是静态字段则为 null
     * @param field 要获取的字段
     * @param <T>   返回值类型
     * @return 字段值
     * @throws Throwable 如果获取失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object src, Field field) throws Throwable {
        MethodHandle methodHandle = lookup.unreflectGetter(field);
        Object result;
        if (Modifier.isStatic(field.getModifiers())) {
            result = methodHandle.invokeWithArguments();
        } else {
            result = methodHandle.bindTo(src).invokeWithArguments();
        }
        return (T) result;
    }

    /**
     * 安全地设置字段值（捕获异常）
     *
     * @param src   源对象，如果是静态字段则为 null
     * @param field 要设置的字段
     * @param value 要设置的值
     * @return 是否设置成功
     */
    public static boolean putSafely(Object src, Field field, Object value) {
        try {
            put(src, field, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 安全地获取字段值（捕获异常）
     *
     * @param src   源对象，如果是静态字段则为 null
     * @param field 要获取的字段
     * @param <T>   返回值类型
     * @return 字段值，如果获取失败则返回 null
     */
    public static <T> T getSafely(Object src, Field field) {
        try {
            return get(src, field);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 通过字段名获取字段值
     *
     * @param src       源对象
     * @param fieldName 字段名
     * @param <T>       返回值类型
     * @return 字段值
     * @throws Throwable 如果获取失败
     */
    public static <T> T get(Object src, String fieldName) throws Throwable {
        Field field = src.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return get(src, field);
    }

    /**
     * 通过字段名设置字段值
     *
     * @param src       源对象
     * @param fieldName 字段名
     * @param value     要设置的值
     * @throws Throwable 如果设置失败
     */
    public static void put(Object src, String fieldName, Object value) throws Throwable {
        Field field = src.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        put(src, field, value);
    }

    /**
     * 安全地通过字段名获取字段值
     *
     * @param src       源对象
     * @param fieldName 字段名
     * @param <T>       返回值类型
     * @return 字段值，如果获取失败则返回 null
     */
    public static <T> T getSafely(Object src, String fieldName) {
        try {
            return get(src, fieldName);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 安全地通过字段名设置字段值
     *
     * @param src       源对象
     * @param fieldName 字段名
     * @param value     要设置的值
     * @return 是否设置成功
     */
    public static boolean putSafely(Object src, String fieldName, Object value) {
        try {
            put(src, fieldName, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
} 