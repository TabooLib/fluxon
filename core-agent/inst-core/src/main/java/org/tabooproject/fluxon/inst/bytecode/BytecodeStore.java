package org.tabooproject.fluxon.inst.bytecode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 原始字节码存储。
 * 缓存类的原始字节码，确保注入可以完全还原。
 */
public class BytecodeStore {

    private static final BytecodeStore INSTANCE = new BytecodeStore();

    // 类名 -> 原始字节码
    private final Map<String, byte[]> originalBytecode = new ConcurrentHashMap<>();

    private BytecodeStore() {
    }

    public static BytecodeStore getInstance() {
        return INSTANCE;
    }

    /**
     * 缓存类的原始字节码。
     * 仅在首次转换时调用。
     * 
     * @param className 内部类名（使用 '/' 分隔）
     * @param bytecode 原始字节码
     */
    public void store(String className, byte[] bytecode) {
        originalBytecode.putIfAbsent(className, bytecode.clone());
    }

    /**
     * 获取类的原始字节码。
     * 
     * @param className 内部类名
     * @return 原始字节码，如果不存在则返回 null
     */
    public byte[] get(String className) {
        byte[] bytes = originalBytecode.get(className);
        return bytes != null ? bytes.clone() : null;
    }

    /**
     * 检查是否存在某类的原始字节码缓存。
     */
    public boolean contains(String className) {
        return originalBytecode.containsKey(className);
    }

    /**
     * 检查是否存在某类的原始字节码缓存。
     * 与 {@link #contains(String)} 相同，提供更简洁的 API。
     */
    public boolean has(String className) {
        return contains(className);
    }

    /**
     * 移除某类的原始字节码缓存。
     * 当该类的所有注入都被撤销时调用。
     */
    public void remove(String className) {
        originalBytecode.remove(className);
    }

    /**
     * 清空所有缓存。
     */
    public void clear() {
        originalBytecode.clear();
    }

    /**
     * 获取当前缓存的类数量。
     */
    public int size() {
        return originalBytecode.size();
    }
}
