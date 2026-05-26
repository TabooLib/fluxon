package org.tabooproject.fluxon.runtime;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getObjectType;

public class Type {

    public static final Type SELF = new Type(Type.class);
    public static final Type VOID = new Type(void.class);
    public static final Type OBJECT = new Type(Object.class);
    public static final Type NUMBER = new Type(Number.class);
    public static final Type STRING = new Type(String.class);
    public static final Type MATH = new Type(Math.class);
    public static final Type ERROR = new Type(Error.class);
    public static final Type RUNTIME_EXCEPTION = new Type(RuntimeException.class);
    public static final Type ILLEGAL_ARGUMENT_EXCEPTION = new Type(IllegalArgumentException.class);
    public static final Type THROWABLE = new Type(Throwable.class);
    public static final Type PRINT_STREAM = new Type(PrintStream.class);
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
    // 维度
    private final int dimension;
    // 元素类型（用于容器类型如 List、Range）
    private final Type elementType;
    // 类路径
    private final String path;
    // 类签名
    private final String descriptor;

    static {
        TYPE_MAP.put(void.class, VOID);
        TYPE_MAP.put(Object.class, OBJECT);
        TYPE_MAP.put(Number.class, NUMBER);
        TYPE_MAP.put(String.class, STRING);
        TYPE_MAP.put(Math.class, MATH);
        TYPE_MAP.put(Error.class, ERROR);
        TYPE_MAP.put(RuntimeException.class, RUNTIME_EXCEPTION);
        TYPE_MAP.put(IllegalArgumentException.class, ILLEGAL_ARGUMENT_EXCEPTION);
        TYPE_MAP.put(Throwable.class, THROWABLE);
        TYPE_MAP.put(PrintStream.class, PRINT_STREAM);
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
        TYPE_MAP.put(Class.class, CLASS);
        TYPE_MAP.put(ClassLoader.class, CLASS_LOADER);
        TYPE_MAP.put(Package.class, PACKAGE);
        TYPE_MAP.put(Constructor.class, CONSTRUCTOR);
        TYPE_MAP.put(Method.class, METHOD);
        TYPE_MAP.put(Field.class, FIELD);
        TYPE_MAP.put(File.class, FILE);
        TYPE_MAP.put(Path.class, PATH);
        TYPE_MAP.put(List.class, LIST);
        TYPE_MAP.put(Map.class, MAP);
    }

    public Type(Class<?> source) {
        this(source, 0, null);
    }

    public Type(Class<?> source, int dimension) {
        this(source, dimension, null);
    }

    public Type(Class<?> source, int dimension, Type elementType) {
        this.source = source;
        this.dimension = dimension;
        this.elementType = elementType;
        // 获取类路径
        this.path = getInternalName(source);
        // 获取类签名
        StringBuilder descriptor = new StringBuilder(org.objectweb.asm.Type.getDescriptor(source));
        for (int i = 0; i < dimension; i++) {
            descriptor.insert(0, "[");
        }
        this.descriptor = descriptor.toString();
    }

    /**
     * 创建带元素类型的新 Type
     */
    public Type withElementType(Type elementType) {
        return new Type(this.source, this.dimension, elementType);
    }

    /**
     * 获取元素类型
     */
    public Type getElementType() {
        return elementType;
    }

    /**
     * 是否有元素类型信息
     */
    public boolean hasElementType() {
        return elementType != null;
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

    public static int readAsInt(long bits, Type type) {
        if (type == I || type == Z || type == J) return (int) bits;
        if (type == F) return (int) Float.intBitsToFloat((int) bits);
        if (type == D) return (int) Double.longBitsToDouble(bits);
        throw new IllegalArgumentException("Cannot read as int: " + type);
    }

    public static long readAsLong(long bits, Type type) {
        if (type == I || type == Z) return (int) bits;
        if (type == J) return bits;
        if (type == F) return (long) Float.intBitsToFloat((int) bits);
        if (type == D) return (long) Double.longBitsToDouble(bits);
        throw new IllegalArgumentException("Cannot read as long: " + type);
    }

    public static float readAsFloat(long bits, Type type) {
        if (type == F) return Float.intBitsToFloat((int) bits);
        return (float) readAsDouble(bits, type);
    }

    public static double readAsDouble(long bits, Type type) {
        if (type == D) return Double.longBitsToDouble(bits);
        if (type == F) return Float.intBitsToFloat((int) bits);
        if (type == I || type == Z) return (int) bits;
        if (type == J) return bits;
        throw new IllegalArgumentException("Cannot read as double: " + type);
    }

    public static boolean readAsBoolean(long bits, Type type) {
        if (type == D) return Double.longBitsToDouble(bits) != 0D;
        if (type == F) return Float.intBitsToFloat((int) bits) != 0F;
        if (type == I || type == Z || type == J) return bits != 0L;
        throw new IllegalArgumentException("Cannot read as boolean: " + type);
    }

    /**
     * 脚本里的 Java 类名是点分形式，生成 Class 常量前统一转换成 ASM object type。
     */
    public static Object asmObjectType(String className) {
        return getObjectType(className.replace('.', '/'));
    }

    /**
     * 将包装类型拆箱为 long 位模式
     */
    public static long unbox(Object value, Type type) {
        if (type == I || type == Z) {
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        }
        if (type == J) return ((Number) value).longValue();
        if (type == D) return Double.doubleToRawLongBits(((Number) value).doubleValue());
        if (type == F) return Float.floatToRawIntBits(((Number) value).floatValue());
        throw new IllegalArgumentException("Cannot unbox type: " + type);
    }

    /**
     * 将 Java Class 转换为对应的 Type
     * 用于参数类型注解的类型映射
     */
    public static Type fromClass(Class<?> clazz) {
        return TYPE_MAP.computeIfAbsent(clazz, Type::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Type)) return false;
        Type type = (Type) o;
        return dimension == type.dimension && source == type.source;
    }

    @Override
    public int hashCode() {
        return source.hashCode() * 31 + dimension;
    }

    @Override
    public String toString() {
        return descriptor;
    }
}
