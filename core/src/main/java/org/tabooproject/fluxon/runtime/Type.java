package org.tabooproject.fluxon.runtime;

public class Type {

    public static final Type OBJECT = new Type(Object.class);
    public static final Type STRING = new Type(String.class);
    public static final Type INT = new Type(Integer.class);
    public static final Type LONG = new Type(Long.class);
    public static final Type FLOAT = new Type(Float.class);
    public static final Type DOUBLE = new Type(Double.class);
    public static final Type BOOLEAN = new Type(Boolean.class);
    public static final Type I = new Type(int.class);
    public static final Type J = new Type(long.class);
    public static final Type F = new Type(float.class);
    public static final Type D = new Type(double.class);
    public static final Type Z = new Type(boolean.class);

    // 类对象
    private final Class<?> source;
    // 纬度
    private final int dimension;
    // 类路径
    private final String path;
    // 类签名
    private final String descriptor;

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
        this.descriptor = descriptor.toString();
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

    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return descriptor;
    }
}
