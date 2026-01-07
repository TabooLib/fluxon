package org.tabooproject.fluxon.runtime.collection;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * Copy-On-Write Map 实现
 * <p>
 * 用于替代 {@code new HashMap<>(source)} 的防御性复制模式。
 * 构造时 O(1) 仅保存引用，首次写入时才执行真正的复制。
 * <p>
 * 适用场景：
 * <ul>
 *   <li>源 Map 在创建后很少被修改</li>
 *   <li>需要频繁创建 Map 副本但大部分副本从不被修改</li>
 *   <li>读多写少的环境（如 Environment 的 functions/variables）</li>
 * </ul>
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public final class CopyOnWriteMap<K, V> extends AbstractMap<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源：
     * - detached=false 时指向共享的原始 Map
     * - detached=true 时指向本地拥有的 HashMap
     */
    private Map<K, V> data;

    /**
     * 是否已脱离共享（已执行复制）
     */
    private boolean detached;

    /**
     * 从现有 Map 创建 COW 包装器
     * <p>
     * 注意：此构造器不会复制数据，调用方需确保在 CowMap 生命周期内
     * 不会直接修改原始 Map（或接受可能的不一致性）。
     *
     * @param source 原始 Map（将被共享引用）
     */
    public CopyOnWriteMap(Map<K, V> source) {
        this.data = Objects.requireNonNull(source, "source");
        this.detached = false;
    }

    /**
     * 创建空的 CowMap（立即 detached）
     */
    public CopyOnWriteMap() {
        this.data = new HashMap<>();
        this.detached = true;
    }

    /**
     * 创建指定初始容量的空 CowMap（立即 detached）
     *
     * @param initialCapacity 初始容量
     */
    public CopyOnWriteMap(int initialCapacity) {
        this.data = new HashMap<>(initialCapacity);
        this.detached = true;
    }

    /**
     * 检查是否已脱离共享
     *
     * @return 如果已执行过复制返回 true
     */
    public boolean isDetached() {
        return detached;
    }

    /**
     * 强制脱离共享（执行复制）
     * <p>
     * 如果已经 detached，此方法无操作。
     * 可用于预热场景，避免写入时的延迟。
     */
    public void detach() {
        if (!detached) {
            data = new HashMap<>(data);
            detached = true;
        }
    }

    /**
     * 确保可写（必要时执行复制）
     */
    private void ensureWritable() {
        if (!detached) {
            detach();
        }
    }

    // ==================== 读操作（直接委托） ====================

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return data.get(key);
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        // 返回不可变视图，避免通过 keySet 修改
        if (!detached) {
            return Collections.unmodifiableSet(data.keySet());
        }
        return data.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        // 返回不可变视图，避免通过 values 修改
        if (!detached) {
            return Collections.unmodifiableCollection(data.values());
        }
        return data.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        // 返回不可变视图，避免通过 entrySet 修改
        if (!detached) {
            return Collections.unmodifiableSet(data.entrySet());
        }
        return data.entrySet();
    }

    // ==================== 写操作（触发复制） ====================

    @Override
    public V put(K key, V value) {
        ensureWritable();
        return data.put(key, value);
    }

    @Override
    public V remove(Object key) {
        ensureWritable();
        return data.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m.isEmpty()) {
            return;
        }
        ensureWritable();
        data.putAll(m);
    }

    @Override
    public void clear() {
        if (isEmpty()) {
            return;
        }
        ensureWritable();
        data.clear();
    }

    // ==================== Java 8+ 默认方法覆盖 ====================

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        // 只有实际需要 put 时才 detach
        if (data.containsKey(key)) {
            return data.get(key);
        }
        ensureWritable();
        return data.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        // 只有实际需要 remove 时才 detach
        if (!Objects.equals(data.get(key), value) || !data.containsKey(key)) {
            return false;
        }
        ensureWritable();
        return data.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        // 只有实际需要 replace 时才 detach
        if (!Objects.equals(data.get(key), oldValue) || !data.containsKey(key)) {
            return false;
        }
        ensureWritable();
        return data.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        // 只有 key 存在时才 detach
        if (!data.containsKey(key)) {
            return null;
        }
        ensureWritable();
        return data.replace(key, value);
    }

    // ==================== 工厂方法 ====================

    /**
     * 从现有 Map 创建 COW 包装器
     *
     * @param source 原始 Map
     * @param <K>    键类型
     * @param <V>    值类型
     * @return CowMap 实例
     */
    public static <K, V> CopyOnWriteMap<K, V> wrap(Map<K, V> source) {
        return new CopyOnWriteMap<>(source);
    }

    /**
     * 创建空的 CowMap
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 空的 CowMap 实例
     */
    public static <K, V> CopyOnWriteMap<K, V> empty() {
        return new CopyOnWriteMap<>();
    }
}
