package org.tabooproject.fluxon.runtime.sharing;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JVM 全局函数共享注册表。
 * 使用 System.getProperties() 作为进程级单例锚点，
 * 实现跨 ClassLoader 的函数共享。
 * <p>
 * Key 格式: "owner:functionName"（普通函数）或 "owner:functionName@className"（扩展函数）
 * Value: Object[] 元组（见 SharedFunctionEntry）
 *
 * @author sky
 */
public final class SharedFunctionRegistry {

    private static final String PROPERTY_KEY = "fluxon.shared-functions";

    /**
     * 获取全局注册表实例
     */
    @SuppressWarnings("unchecked")
    public static ConcurrentHashMap<String, Object[]> getGlobalRegistry() {
        return (ConcurrentHashMap<String, Object[]>) System.getProperties().computeIfAbsent(PROPERTY_KEY, k -> new ConcurrentHashMap<String, Object[]>());
    }

    /**
     * 注册普通共享函数
     */
    public static void register(String owner, String name, MethodHandle handle) {
        getGlobalRegistry().put(owner + ":" + name, SharedFunctionEntry.create(name, owner, handle, false, null));
    }

    /**
     * 注册共享扩展函数
     */
    public static void registerExtension(String owner, String name, MethodHandle handle, Class<?> extensionTarget) {
        getGlobalRegistry().put(owner + ":" + name + "@" + extensionTarget.getName(), SharedFunctionEntry.create(name, owner, handle, true, extensionTarget));
    }

    /**
     * 查找普通共享函数
     */
    public static Object[] find(String owner, String name) {
        return getGlobalRegistry().get(owner + ":" + name);
    }

    /**
     * 查找共享扩展函数
     */
    public static Object[] findExtension(String owner, String name, Class<?> extensionTarget) {
        return getGlobalRegistry().get(owner + ":" + name + "@" + extensionTarget.getName());
    }

    /**
     * 按函数名查找所有匹配的共享函数（跨所有 owner）
     */
    public static List<Object[]> findAll(String name) {
        List<Object[]> results = new ArrayList<>();
        for (Map.Entry<String, Object[]> entry : getGlobalRegistry().entrySet()) {
            String key = entry.getKey();
            int colon = key.indexOf(':');
            if (colon >= 0) {
                String entryName = key.substring(colon + 1);
                int at = entryName.indexOf('@');
                if (at >= 0) entryName = entryName.substring(0, at);
                if (entryName.equals(name)) {
                    results.add(entry.getValue());
                }
            }
        }
        return results;
    }

    /**
     * 移除指定 owner 的所有共享函数
     */
    public static int unregisterAll(String owner) {
        String prefix = owner + ":";
        int count = 0;
        java.util.Iterator<Map.Entry<String, Object[]>> it = getGlobalRegistry().entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getKey().startsWith(prefix)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * 获取所有已注册 owner
     */
    public static List<String> getOwners() {
        List<String> owners = new ArrayList<>();
        for (String key : getGlobalRegistry().keySet()) {
            int colon = key.indexOf(':');
            if (colon >= 0) {
                String owner = key.substring(0, colon);
                if (!owners.contains(owner)) {
                    owners.add(owner);
                }
            }
        }
        return owners;
    }
}
