package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.interpreter.Interpreter;

/**
 * 函数调用上下文
 * 封装函数调用所需的所有信息：目标对象、参数列表和环境
 *
 * @author sky
 */
public final class FunctionContext<Target> implements AutoCloseable {

    public static final Type TYPE = new Type(FunctionContext.class);

    private static final Object[] EMPTY_REFS = new Object[0];
    private static final long[] EMPTY_PRIMITIVES = new long[0];
    private static final byte[] EMPTY_ARG_TYPES = new byte[0];

    // 参数类型标记常量
    static final byte TYPE_REF = 0;
    static final byte TYPE_INT = 'I';
    static final byte TYPE_LONG = 'J';
    static final byte TYPE_FLOAT = 'F';
    static final byte TYPE_DOUBLE = 'D';
    static final byte TYPE_BOOL = 'Z';

    private Function function;
    private Target target;
    private Environment environment;
    private FunctionContextPool pool;
    private Interpreter interpreter;
    int stackIndex = -1;

    private long[] primitives;
    private Object[] refs;
    private byte[] argTypes;
    private int capacity;
    private int argumentCount;

    public long returnPrimitive;
    public Object returnRef;
    public Type returnType;

    /**
     * 池专用构造函数，字段在 reset 时初始化
     */
    FunctionContext(@NotNull FunctionContextPool pool) {
        this.pool = pool;
        this.refs = EMPTY_REFS;
        this.primitives = EMPTY_PRIMITIVES;
        this.argTypes = EMPTY_ARG_TYPES;
        this.capacity = 0;
    }

    /**
     * 栈槽位构造函数，stackIndex 固化到构造期
     */
    FunctionContext(@NotNull FunctionContextPool pool, int capacity, int stackIndex) {
        this.pool = pool;
        this.primitives = new long[capacity];
        this.refs = new Object[capacity];
        this.argTypes = new byte[capacity];
        this.capacity = capacity;
        this.stackIndex = stackIndex;
    }

    public int getInt(int index) {
        return (int) primitives[index];
    }

    public long getLong(int index) {
        return primitives[index];
    }

    public double getDouble(int index) {
        return Double.longBitsToDouble(primitives[index]);
    }

    public float getFloat(int index) {
        return Float.intBitsToFloat((int) primitives[index]);
    }

    public boolean getBool(int index) {
        return primitives[index] != 0;
    }

    public long getPrimitive(int index) {
        return primitives[index];
    }

    public Object getRef(int index) {
        return refs[index];
    }

    public String getString(int index) {
        return (String) refs[index];
    }

    public void setInt(int index, int v) {
        primitives[index] = v;
        argTypes[index] = TYPE_INT;
    }

    public void setLong(int index, long v) {
        primitives[index] = v;
        argTypes[index] = TYPE_LONG;
    }

    public void setDouble(int index, double v) {
        primitives[index] = Double.doubleToRawLongBits(v);
        argTypes[index] = TYPE_DOUBLE;
    }

    public void setFloat(int index, float v) {
        primitives[index] = Float.floatToRawIntBits(v);
        argTypes[index] = TYPE_FLOAT;
    }

    public void setBool(int index, boolean v) {
        primitives[index] = v ? 1 : 0;
        argTypes[index] = TYPE_BOOL;
    }

    public void setRef(int index, Object v) {
        refs[index] = v;
        argTypes[index] = TYPE_REF;
    }

    public boolean isArgPrimitive(int index) {
        return index < argTypes.length && argTypes[index] != TYPE_REF;
    }

    public byte getArgType(int index) {
        return index < argTypes.length ? argTypes[index] : TYPE_REF;
    }

    public double getAsDouble(int index) {
        byte t = index < argTypes.length ? argTypes[index] : TYPE_REF;
        if (t == TYPE_REF) return ((Number) refs[index]).doubleValue();
        switch (t) {
            case TYPE_LONG:
                return (double) primitives[index];
            case TYPE_FLOAT:
                return Float.intBitsToFloat((int) primitives[index]);
            case TYPE_DOUBLE:
                return Double.longBitsToDouble(primitives[index]);
            default:
                return (int) primitives[index]; // I, Z
        }
    }

    public int getAsInt(int index) {
        byte t = index < argTypes.length ? argTypes[index] : TYPE_REF;
        if (t == TYPE_REF) return ((Number) refs[index]).intValue();
        switch (t) {
            case TYPE_LONG:
                return (int) primitives[index];
            case TYPE_FLOAT:
                return (int) Float.intBitsToFloat((int) primitives[index]);
            case TYPE_DOUBLE:
                return (int) Double.longBitsToDouble(primitives[index]);
            default:
                return (int) primitives[index]; // I, Z
        }
    }

    public long getAsLong(int index) {
        byte t = index < argTypes.length ? argTypes[index] : TYPE_REF;
        if (t == TYPE_REF) return ((Number) refs[index]).longValue();
        switch (t) {
            case TYPE_INT:
                return (int) primitives[index]; // sign-extend
            case TYPE_FLOAT:
                return (long) Float.intBitsToFloat((int) primitives[index]);
            case TYPE_DOUBLE:
                return (long) Double.longBitsToDouble(primitives[index]);
            default:
                return primitives[index]; // J, Z
        }
    }

    public float getAsFloat(int index) {
        byte t = index < argTypes.length ? argTypes[index] : TYPE_REF;
        if (t == TYPE_REF) return ((Number) refs[index]).floatValue();
        switch (t) {
            case TYPE_LONG:
                return (float) primitives[index];
            case TYPE_DOUBLE:
                return (float) Double.longBitsToDouble(primitives[index]);
            case TYPE_FLOAT:
                return Float.intBitsToFloat((int) primitives[index]);
            default:
                return (int) primitives[index]; // I, Z
        }
    }

    public boolean getAsBoolean(int index) {
        byte t = index < argTypes.length ? argTypes[index] : TYPE_REF;
        if (t == TYPE_REF) {
            Object ref = refs[index];
            if (ref instanceof Boolean) return (Boolean) ref;
            if (ref instanceof Number) return ((Number) ref).doubleValue() != 0;
            return ref != null;
        }
        switch (t) {
            case TYPE_DOUBLE:
                return Double.longBitsToDouble(primitives[index]) != 0;
            case TYPE_FLOAT:
                return Float.intBitsToFloat((int) primitives[index]) != 0;
            default:
                return primitives[index] != 0; // I, J, Z
        }
    }

    public Object getArgBoxed(int index) {
        if (index >= argumentCount) return null;
        byte t = index < argTypes.length ? argTypes[index] : TYPE_REF;
        if (t == TYPE_REF) return refs[index];
        switch (t) {
            case TYPE_INT:
                return (int) primitives[index];
            case TYPE_LONG:
                return primitives[index];
            case TYPE_FLOAT:
                return Float.intBitsToFloat((int) primitives[index]);
            case TYPE_DOUBLE:
                return Double.longBitsToDouble(primitives[index]);
            case TYPE_BOOL:
                return primitives[index] != 0;
            default:
                return refs[index];
        }
    }

    public void setReturnInt(int v) {
        returnPrimitive = v;
        returnType = Type.I;
    }

    public void setReturnLong(long v) {
        returnPrimitive = v;
        returnType = Type.J;
    }

    public void setReturnDouble(double v) {
        returnPrimitive = Double.doubleToRawLongBits(v);
        returnType = Type.D;
    }

    public void setReturnFloat(float v) {
        returnPrimitive = Float.floatToRawIntBits(v);
        returnType = Type.F;
    }

    public void setReturnBool(boolean v) {
        returnPrimitive = v ? 1 : 0;
        returnType = Type.Z;
    }

    public void setReturnRef(Object v) {
        returnRef = v;
        returnType = Type.OBJECT;
    }

    public long getReturnPrimitive() {
        return returnPrimitive;
    }

    public Object getReturnRef() {
        // 当返回值是原始类型时按需装箱，保证所有直接调用 getReturnRef() 的调用方正确
        if (returnType != null && returnType.isPrimitive()) {
            return Type.box(returnPrimitive, returnType);
        }
        return returnRef;
    }

    public Type getReturnType() {
        return returnType;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    /**
     * 热循环复用 context，更新引用参数
     */
    public void updateRefs(Object... args) {
        this.refs = args;
        this.argumentCount = args.length;
    }

    @NotNull
    public Function getFunction() {
        return function;
    }

    @Nullable
    public Target getTarget() {
        return target;
    }

    @NotNull
    public Environment getEnvironment() {
        return environment;
    }

    @NotNull
    public FunctionContextPool getPool() {
        return pool;
    }

    /**
     * 设置关联的解释器（解释执行时使用）
     */
    public void setInterpreter(@Nullable Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * 获取关联的解释器
     */
    @Nullable
    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     * 设置函数并根据签名转换参数类型（延迟解析使用）
     *
     * @param resolved    解析后的函数
     * @param actualTypes 实际参数类型
     */
    public void setFunctionAndConvertArgs(Function resolved, Type[] actualTypes) {
        this.function = resolved;
        FunctionSignature sig = resolved.getSignature();
        if (sig != null) {
            Type[] expectedTypes = sig.getParameterTypes();
            for (int i = 0; i < argumentCount && i < expectedTypes.length; i++) {
                convertArgType(i, actualTypes[i], expectedTypes[i]);
            }
        }
    }

    /**
     * 收集实际参数类型
     */
    public Type[] collectArgTypes() {
        Type[] types = new Type[argumentCount];
        for (int i = 0; i < argumentCount; i++) {
            byte t = argTypes[i];
            if (t == TYPE_REF) {
                Object ref = refs[i];
                types[i] = ref != null ? Type.fromClass(ref.getClass()) : Type.OBJECT;
            } else {
                types[i] = primitiveByteToType(t);
            }
        }
        return types;
    }

    /**
     * 转换参数类型以匹配期望类型
     */
    private void convertArgType(int index, Type actual, Type expected) {
        byte t = argTypes[index];
        // 即使类型相等，如果存储方式不匹配也要转换（例如 Double 对象存在 refs 但期望 primitive）
        if (t == TYPE_REF) {
            if (expected.isPrimitive()) {
                Object ref = refs[index];
                if (ref instanceof Boolean) {
                    boolean value = (Boolean) ref;
                    String desc = expected.getDescriptor();
                    if ("Z".equals(desc)) {
                        setBool(index, value);
                    } else if ("I".equals(desc)) {
                        setInt(index, value ? 1 : 0);
                    } else if ("J".equals(desc)) {
                        setLong(index, value ? 1L : 0L);
                    } else if ("F".equals(desc)) {
                        setFloat(index, value ? 1F : 0F);
                    } else if ("D".equals(desc)) {
                        setDouble(index, value ? 1D : 0D);
                    }
                } else if (ref instanceof Number) {
                    Number num = (Number) ref;
                    String desc = expected.getDescriptor();
                    if ("I".equals(desc)) {
                        setInt(index, num.intValue());
                    } else if ("Z".equals(desc)) {
                        setInt(index, num.intValue());
                    } else if ("J".equals(desc)) {
                        setLong(index, num.longValue());
                    } else if ("F".equals(desc)) {
                        setFloat(index, num.floatValue());
                    } else if ("D".equals(desc)) {
                        setDouble(index, num.doubleValue());
                    }
                }
            }
        } else if (expected.isPrimitive() && !actual.equals(expected)) {
            // 原始类型之间的转换
            double value = getAsDouble(index);
            String desc = expected.getDescriptor();
            if ("I".equals(desc)) {
                setInt(index, (int) value);
            } else if ("Z".equals(desc)) {
                setInt(index, (int) value);
            } else if ("J".equals(desc)) {
                setLong(index, (long) value);
            } else if ("F".equals(desc)) {
                setFloat(index, (float) value);
            } else if ("D".equals(desc)) {
                setDouble(index, value);
            }
        }
    }

    /**
     * 将参数类型字节转换为 Type
     */
    private static Type primitiveByteToType(byte t) {
        switch (t) {
            case TYPE_INT: return Type.I;
            case TYPE_LONG: return Type.J;
            case TYPE_FLOAT: return Type.F;
            case TYPE_DOUBLE: return Type.D;
            case TYPE_BOOL: return Type.Z;
            default: return Type.OBJECT;
        }
    }

    @SuppressWarnings("unchecked")
    void reset(
            @NotNull Function function,
            @Nullable Object target,
            @NotNull Object[] refs,
            @NotNull Environment environment) {
        this.function = function;
        this.target = (Target) target;
        this.refs = refs;
        this.argumentCount = refs.length;
        this.argTypes = EMPTY_ARG_TYPES;
        this.capacity = 0;
        this.environment = environment;
        this.returnRef = null;
        this.returnType = null;
        this.interpreter = null;
    }

    @SuppressWarnings("unchecked")
    void reset(
            @NotNull Function function,
            @Nullable Object target,
            int argCount,
            @NotNull Environment environment) {
        this.function = function;
        this.target = (Target) target;
        ensureCapacity(argCount);
        this.argumentCount = argCount;
        this.environment = environment;
        this.returnRef = null;
        this.returnType = null;
        this.interpreter = null;
    }

    /**
     * 脚本执行结束后清理空闲槽引用，避免线程池复用线程时保留上一轮变量图
     */
    void clearIdleReferences() {
        function = null;
        target = null;
        environment = null;
        interpreter = null;
        returnRef = null;
        returnPrimitive = 0L;
        returnType = null;
        if (capacity == 0) {
            refs = EMPTY_REFS;
            primitives = EMPTY_PRIMITIVES;
            argTypes = EMPTY_ARG_TYPES;
        } else {
            for (int i = 0; i < argumentCount && i < refs.length; i++) {
                refs[i] = null;
            }
        }
        argumentCount = 0;
    }

    /**
     * 从池中分离（用于 async 转移所有权）
     * 用新 context 替换自己在栈中的槽位，使 close() 无需 detached 检查
     */
    public void detachFromPool() {
        pool.detach(this);
    }

    /**
     * 将 pool 引用重新绑定到当前执行线程的 ThreadLocal pool
     * async/primarySync 函数在 worker 线程开始执行前调用，
     * 防止嵌套调用通过 ctx.getPool() 拿到调用方线程的 pool 造成跨线程竞态
     */
    public void reassignPool() {
        pool = FunctionContextPool.local();
    }

    /**
     * 确保容量足够存储指定数量的局部变量（env-free 解释器路径使用）
     * 不修改 argumentCount，仅保证内部数组不越界
     *
     * @param count 所需最小容量
     */
    public void ensureLocalCapacity(int count) {
        if (capacity < count) {
            long[] newPrimitives = new long[count];
            Object[] newRefs = new Object[count];
            byte[] newArgTypes = new byte[count];
            System.arraycopy(primitives, 0, newPrimitives, 0, capacity);
            System.arraycopy(refs, 0, newRefs, 0, capacity);
            System.arraycopy(argTypes, 0, newArgTypes, 0, capacity);
            primitives = newPrimitives;
            refs = newRefs;
            argTypes = newArgTypes;
            capacity = count;
        }
    }

    /**
     * 将原始类型参数统一装箱到 refs 数组（env-free 解释器路径使用）
     * 调用后所有参数位置的 argTypes 均为 TYPE_REF，getLocal 可直接读 refs
     *
     * @param paramCount 参数数量
     */
    public void normalizeArgsToRef(int paramCount) {
        for (int i = 0; i < paramCount && i < argTypes.length; i++) {
            byte t = argTypes[i];
            if (t != TYPE_REF) {
                switch (t) {
                    case TYPE_INT: refs[i] = (int) primitives[i]; break;
                    case TYPE_LONG: refs[i] = primitives[i]; break;
                    case TYPE_FLOAT: refs[i] = Float.intBitsToFloat((int) primitives[i]); break;
                    case TYPE_DOUBLE: refs[i] = Double.longBitsToDouble(primitives[i]); break;
                    case TYPE_BOOL: refs[i] = primitives[i] != 0; break;
                }
                argTypes[i] = TYPE_REF;
            }
        }
    }

    /**
     * 获取指定位置的值（env-free 解释器路径使用）
     * 调用 normalizeArgsToRef 后所有位置均为 TYPE_REF，直接读 refs
     *
     * @param index 位置索引
     * @return 值
     */
    public Object getLocal(int index) {
        return refs[index];
    }

    /**
     * 设置指定位置的值（env-free 解释器路径使用）
     *
     * @param index 位置索引
     * @param value 值
     */
    public void setLocal(int index, Object value) {
        refs[index] = value;
    }

    private void ensureCapacity(int count) {
        if (capacity < count) {
            primitives = new long[count];
            refs = new Object[count];
            argTypes = new byte[count];
            capacity = count;
        } else if (capacity > count) {
            // 清除 count 之后的旧引用防止 GC 泄漏
            for (int i = count; i < capacity; i++) {
                refs[i] = null;
            }
        }
    }

    @Override
    public String toString() {
        return "FunctionContext{" +
                "function=" + function.getName() +
                ", target=" + target +
                ", arguments=" + argumentCount +
                '}';
    }

    @Override
    public void close() {
        pool.releaseUnchecked(this);
    }
}
