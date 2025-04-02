package org.tabooproject.fluxon.runtime;

public class Type {

    // 类对象
    private final Class<?> source;
    // 纬度
    private final int dimension;
    // 类路径
    private final String path;
    // 类签名
    private final String signature;

    public Type(Class<?> source) {
        this(source, 0);
    }

    public Type(Class<?> source, int dimension) {
        this.source = source;
        this.dimension = dimension;
        // 获取类路径
        this.path = source.getName().replace('.', '/');
        // 获取类签名
        StringBuilder descriptor = new StringBuilder(org.objectweb.asm.Type.getDescriptor(source));
        for (int i = 0; i < dimension; i++) {
            descriptor.insert(0, "[");
        }
        this.signature = descriptor.toString();
    }

    /**
     * 是否为数组类型
     * 当 dimension > 0 时，表示数组类型
     */
    public boolean isArray() {
        return dimension > 0;
    }

    /**
     * 是否为基本类型
     */
    public boolean isPrimitive() {
        return source.isPrimitive();
    }

    public Class<?> getSource() {
        return source;
    }

    public int getDimension() {
        return dimension;
    }

    public String getPath() {
        return path;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return signature;
    }
}
