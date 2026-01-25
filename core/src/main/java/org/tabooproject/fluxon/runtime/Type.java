package org.tabooproject.fluxon.runtime;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Type {

    public static final Type VOID = new Type(void.class);
    public static final Type OBJECT = new Type(Object.class);
    public static final Type NUMBER = new Type(Number.class);
    public static final Type STRING = new Type(String.class);
    public static final Type INT = new Type(Integer.class);
    public static final Type LONG = new Type(Long.class);
    public static final Type FLOAT = new Type(Float.class);
    public static final Type DOUBLE = new Type(Double.class);
    public static final Type BOOLEAN = new Type(Boolean.class);
    public static final Type CLASS = new Type(Class.class);
    public static final Type CLASS_LOADER = new Type(ClassLoader.class);
    public static final Type PACKAGE = new Type(Package.class);
    public static final Type CONSTRUCTOR = new Type(Constructor.class);
    public static final Type METHOD = new Type(Method.class);
    public static final Type FIELD = new Type(Field.class);
    public static final Type FILE = new Type(File.class);
    public static final Type PATH = new Type(Path.class);
    public static final Type LIST = new Type(List.class);
    public static final Type MAP = new Type(Map.class);
    public static final Type I = new Type(int.class);
    public static final Type J = new Type(long.class);
    public static final Type F = new Type(float.class);
    public static final Type D = new Type(double.class);
    public static final Type Z = new Type(boolean.class);

    public static final Map<Class<?>, Type> TYPE_MAP = new ConcurrentHashMap<>();

    // 类对象
    private final Class<?> source;
    // 纬度
    private final int dimension;
    // 类路径
    private final String path;
    // 类签名
    private final String descriptor;

    static {
        TYPE_MAP.put(int.class, I);
        TYPE_MAP.put(Integer.class, I);
        TYPE_MAP.put(long.class, J);
        TYPE_MAP.put(Long.class, J);
        TYPE_MAP.put(float.class, F);
        TYPE_MAP.put(Float.class, F);
        TYPE_MAP.put(double.class, D);
        TYPE_MAP.put(Double.class, D);
        TYPE_MAP.put(boolean.class, Z);
        TYPE_MAP.put(Boolean.class, Z);
        TYPE_MAP.put(void.class, VOID);
    }

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

    /**
     * 将 long 位模式装箱为对应的包装类型
     */
    public static Object box(long bits, Type type) {
        if (type == I) return (int) bits;
        if (type == J) return bits;
        if (type == D) return Double.longBitsToDouble(bits);
        if (type == F) return Float.intBitsToFloat((int) bits);
        if (type == Z) return bits != 0;
        throw new IllegalArgumentException("Cannot box type: " + type);
    }

    /**
     * 将 Java Class 转换为对应的 Type
     * 用于参数类型注解的类型映射
     */
    public static Type fromClass(Class<?> clazz) {
        return TYPE_MAP.computeIfAbsent(clazz, Type::new);
    }

    @Override
    public String toString() {
        return descriptor;
    }
}
