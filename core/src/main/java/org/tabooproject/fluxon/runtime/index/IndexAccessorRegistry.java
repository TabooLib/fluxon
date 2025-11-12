package org.tabooproject.fluxon.runtime.index;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 索引访问器注册表
 * 管理所有注册的索引访问器并提供统一的索引访问接口
 */
public class IndexAccessorRegistry {

    // 单例实例
    private static final IndexAccessorRegistry INSTANCE = new IndexAccessorRegistry();

    // 索引访问器列表
    private final List<IndexAccessor> accessors = new ArrayList<>();

    /**
     * 获取单例实例
     *
     * @return IndexAccessorRegistry 实例
     */
    public static IndexAccessorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 私有构造函数
     * 通过 SPI 加载外部索引访问器
     */
    private IndexAccessorRegistry() {
        // 通过 SPI 加载外部索引访问器
        registerExternalAccessors();
    }

    /**
     * 通过 SPI 加载外部索引访问器
     */
    private void registerExternalAccessors() {
        // 使用 Java SPI 机制加载外部扩展的索引访问器
        ServiceLoader<IndexAccessor> loader = ServiceLoader.load(IndexAccessor.class);
        for (IndexAccessor accessor : loader) {
            accessors.add(accessor);
        }
    }

    /**
     * 注册自定义索引访问器
     *
     * @param accessor 要注册的索引访问器
     */
    public void registerAccessor(IndexAccessor accessor) {
        if (accessor != null && !accessors.contains(accessor)) {
            // 新访问器添加到列表开头，使其优先级高于已有的访问器
            accessors.add(0, accessor);
        }
    }

    /**
     * 尝试获取索引对应的值
     *
     * @param target 目标对象
     * @param index  索引对象
     * @return 如果找到匹配的访问器，返回包含结果的 AccessResult；否则返回 null
     */
    public AccessResult tryGet(Object target, Object index) {
        for (IndexAccessor accessor : accessors) {
            if (accessor.supports(target)) {
                try {
                    Object value = accessor.get(target, index);
                    return AccessResult.success(value);
                } catch (Exception e) {
                    return AccessResult.failure(e);
                }
            }
        }
        return null;
    }

    /**
     * 尝试设置索引对应的值
     *
     * @param target 目标对象
     * @param index  索引对象
     * @param value  要设置的值
     * @return 如果找到匹配的访问器，返回包含结果的 AccessResult；否则返回 null
     */
    public AccessResult trySet(Object target, Object index, Object value) {
        for (IndexAccessor accessor : accessors) {
            if (accessor.supports(target)) {
                if (!accessor.supportsSet()) {
                    return AccessResult.failure(
                        new UnsupportedOperationException("Index set not supported for type: " + target.getClass().getName())
                    );
                }
                try {
                    accessor.set(target, index, value);
                    return AccessResult.success(null);
                } catch (Exception e) {
                    return AccessResult.failure(e);
                }
            }
        }
        return null;
    }

    /**
     * 访问结果封装类
     */
    public static class AccessResult {
        private final boolean success;
        private final Object value;
        private final Exception error;

        private AccessResult(boolean success, Object value, Exception error) {
            this.success = success;
            this.value = value;
            this.error = error;
        }

        public static AccessResult success(Object value) {
            return new AccessResult(true, value, null);
        }

        public static AccessResult failure(Exception error) {
            return new AccessResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public Object getValue() {
            return value;
        }

        public Exception getError() {
            return error;
        }
    }
}

