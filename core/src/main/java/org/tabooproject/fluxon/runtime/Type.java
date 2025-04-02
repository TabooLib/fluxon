package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.Nullable;

public class Type {

    // 类对象
    private final Class<?> source;
    // 纬度
    private final int dimension;

    public Type(Class<?> source) {
        this(source, 0);
    }

    public Type(Class<?> source, int dimension) {
        this.source = source;
        this.dimension = dimension;
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

    /**
     * 获取字节码签名
     */
    public String getSignature() {
        if (source == null) {
            return "Ljava/lang/Object;";
        }
        StringBuilder descriptor = new StringBuilder(org.objectweb.asm.Type.getDescriptor(source));
        for (int i = 0; i < dimension; i++) {
            descriptor.insert(0, "[");
        }
        return descriptor.toString();
    }

    @Nullable
    public Class<?> getSource() {
        return source;
    }

    public int getDimension() {
        return dimension;
    }
}
