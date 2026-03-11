package org.tabooproject.fluxon.runtime.sharing;

import java.lang.invoke.MethodHandle;

/**
 * 共享函数注册表 entry 布局。
 * 实际存储在 JVM 全局 Map 中的是 Object[] 数组（所有元素类型均来自 java.base），
 * 本类提供索引常量和访问方法。
 * <p>
 * Layout v1: [int version, String name, String owner, MethodHandle handle, Boolean isExtension, Class<?> extensionTarget]
 * <p>
 * 版本演进规则：
 * 1. IDX_VERSION 永远是 0，永不改变
 * 2. 新版本只能在 Object[] 末尾追加字段，不能修改已有字段位置
 * 3. 读取方必须先检查版本号，对未知高版本执行优雅降级
 * 4. 低版本读高版本 entry 时，忽略末尾多余字段（数组长度大于预期是安全的）
 *
 * @author sky
 */
public final class SharedFunctionEntry {

    // 当前协议版本
    static final int CURRENT_VERSION = 1;

    // Object[] 索引常量（v1）
    static final int IDX_VERSION = 0;
    static final int IDX_NAME = 1;
    static final int IDX_OWNER = 2;
    static final int IDX_HANDLE = 3;
    static final int IDX_IS_EXTENSION = 4;
    static final int IDX_EXTENSION_TARGET = 5;
    static final int ENTRY_SIZE_V1 = 6;

    /**
     * 创建 entry 元组（当前版本）
     */
    public static Object[] create(String name, String owner, MethodHandle handle, boolean isExtension, Class<?> extensionTarget) {
        Object[] entry = new Object[ENTRY_SIZE_V1];
        entry[IDX_VERSION] = CURRENT_VERSION;
        entry[IDX_NAME] = name;
        entry[IDX_OWNER] = owner;
        entry[IDX_HANDLE] = handle;
        entry[IDX_IS_EXTENSION] = isExtension;
        entry[IDX_EXTENSION_TARGET] = extensionTarget;
        return entry;
    }

    public static int version(Object[] entry) {
        return (int) entry[IDX_VERSION];
    }

    public static boolean isVersionSupported(Object[] entry) {
        int v = (int) entry[IDX_VERSION];
        return v >= 1 && v <= CURRENT_VERSION;
    }

    public static String name(Object[] entry) {
        return (String) entry[IDX_NAME];
    }

    public static String owner(Object[] entry) {
        return (String) entry[IDX_OWNER];
    }

    public static MethodHandle handle(Object[] entry) {
        return (MethodHandle) entry[IDX_HANDLE];
    }

    public static boolean isExtension(Object[] entry) {
        return (Boolean) entry[IDX_IS_EXTENSION];
    }

    public static Class<?> extensionTarget(Object[] entry) {
        return (Class<?>) entry[IDX_EXTENSION_TARGET];
    }
}
