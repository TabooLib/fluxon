package org.tabooproject.fluxon.runtime.index;

/**
 * 示例：自定义类型的索引访问器实现
 *
 * 使用方式：
 *
 * 1. 实现 IndexAccessor 接口
 * 2. 通过 IndexAccessorRegistry.getInstance().registerAccessor(accessor) 注册
 * 3. 或者通过 Java SPI 机制自动加载：
 *    - 在 META-INF/services/ 目录下创建文件：
 *      org.tabooproject.fluxon.runtime.index.IndexAccessor
 *    - 文件内容为实现类的全限定名
 *
 * 示例代码：
 *
 * // 假设有一个自定义类
 * class CustomContainer {
 *     private List<String> data = Arrays.asList("a", "b", "c");
 *     public String getElement(int index) { return data.get(index); }
 *     public void setElement(int index, String value) { data.set(index, value); }
 * }
 *
 * // 为它创建索引访问器
 * class CustomContainerAccessor implements IndexAccessor {
 *
 *     @Override
 *     public boolean supports(Object target) {
 *         return target instanceof CustomContainer;
 *     }
 *
 *     @Override
 *     public Object get(Object target, Object index) throws Exception {
 *         CustomContainer container = (CustomContainer) target;
 *         int idx = ((Number) index).intValue();
 *         return container.getElement(idx);
 *     }
 *
 *     @Override
 *     public void set(Object target, Object index, Object value) throws Exception {
 *         CustomContainer container = (CustomContainer) target;
 *         int idx = ((Number) index).intValue();
 *         container.setElement(idx, (String) value);
 *     }
 * }
 *
 * // 注册访问器
 * IndexAccessorRegistry.getInstance().registerAccessor(new CustomContainerAccessor());
 */
public class IndexAccessorExample {

    private IndexAccessorExample() {
        // 工具类，不允许实例化
    }
}

