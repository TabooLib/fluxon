package org.tabooproject.fluxon.runtime.index;

/**
 * 索引访问器接口
 * 允许第三方为自定义类型实现索引访问功能
 */
public interface IndexAccessor {

    /**
     * 检查此访问器是否支持给定的目标类型
     *
     * @param target 目标对象
     * @return 如果支持返回 true，否则返回 false
     */
    boolean supports(Object target);

    /**
     * 获取索引对应的值
     *
     * @param target 目标对象
     * @param index  索引对象
     * @return 索引对应的值
     * @throws Exception 如果访问失败
     */
    Object get(Object target, Object index) throws Exception;

    /**
     * 设置索引对应的值
     *
     * @param target 目标对象
     * @param index  索引对象
     * @param value  要设置的值
     * @throws Exception 如果设置失败
     */
    void set(Object target, Object index, Object value) throws Exception;

    /**
     * 检查此访问器是否支持设置操作
     * 某些类型可能只支持读取，不支持写入
     *
     * @return 如果支持设置操作返回 true，否则返回 false
     */
    default boolean supportsSet() {
        return true;
    }
}

