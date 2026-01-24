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
public class FunctionContext<Target> implements AutoCloseable {

    public static final Type TYPE = new Type(FunctionContext.class);

    private static final int INITIAL_CAPACITY = 8;
    private static final Object[] EMPTY_REFS = new Object[0];
    private static final long[] EMPTY_PRIMITIVES = new long[0];
    private static final byte[] EMPTY_ARG_TYPES = new byte[0];

    @NotNull
    private Function function;
    @Nullable
    private Target target;
    @NotNull
    private Environment environment;
    @Nullable
    private FunctionContextPool pool;
    @Nullable
    private Interpreter interpreter; // Interpreter 实例，用于解释执行时传递执行上下文

    private long[] primitives;
    private Object[] refs;
    private byte[] argTypes; // 0=ref, 'I'/'J'/'F'/'D'/'Z'=primitive
    private int argumentCount;

    public long returnPrimitive;
    public Object returnRef;
    public Type returnType;

    public FunctionContext(
            @NotNull Function function,
            @Nullable Target target,
            @NotNull Object[] refs,
            @NotNull Environment environment
    ) {
        this(function, target, refs, environment, null);
    }

    public FunctionContext(
            @NotNull Function function,
            @Nullable Target target,
            @NotNull Object[] refs,
            @NotNull Environment environment,
            @Nullable FunctionContextPool pool
    ) {
        this.function = function;
        this.target = target;
        this.refs = refs;
        this.argumentCount = refs.length;
        this.primitives = EMPTY_PRIMITIVES;
        this.argTypes = EMPTY_ARG_TYPES;
        this.environment = environment;
        this.pool = pool;
    }

    // ====================== 参数读取 - 原始类型 ======================

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

    // ====================== 参数读取 - 引用类型 ======================

    public Object getRef(int index) {
        return refs[index];
    }

    // ====================== 参数写入 ======================

    public void setInt(int index, int v) {
        ensurePrimitivesCapacity(index);
        ensureArgTypesCapacity(index);
        primitives[index] = v;
        argTypes[index] = (byte) 'I';
    }

    public void setLong(int index, long v) {
        ensurePrimitivesCapacity(index);
        ensureArgTypesCapacity(index);
        primitives[index] = v;
        argTypes[index] = (byte) 'J';
    }

    public void setDouble(int index, double v) {
        ensurePrimitivesCapacity(index);
        ensureArgTypesCapacity(index);
        primitives[index] = Double.doubleToRawLongBits(v);
        argTypes[index] = (byte) 'D';
    }

    public void setFloat(int index, float v) {
        ensurePrimitivesCapacity(index);
        ensureArgTypesCapacity(index);
        primitives[index] = Float.floatToRawIntBits(v);
        argTypes[index] = (byte) 'F';
    }

    public void setBool(int index, boolean v) {
        ensurePrimitivesCapacity(index);
        ensureArgTypesCapacity(index);
        primitives[index] = v ? 1 : 0;
        argTypes[index] = (byte) 'Z';
    }

    public void setRef(int index, Object v) {
        ensureRefsCapacity(index);
        ensureArgTypesCapacity(index);
        refs[index] = v;
        argTypes[index] = 0;
    }

    // ====================== 参数类型查询 ======================

    public boolean isArgPrimitive(int index) {
        return index < argTypes.length && argTypes[index] != 0;
    }

    public byte getArgType(int index) {
        return index < argTypes.length ? argTypes[index] : 0;
    }

    // ====================== 便捷读取 ======================

    public double getAsDouble(int index) {
        byte t = index < argTypes.length ? argTypes[index] : 0;
        if (t == 0) return ((Number) refs[index]).doubleValue();
        switch (t) {
            case 'J':
                return (double) primitives[index];
            case 'F':
                return Float.intBitsToFloat((int) primitives[index]);
            case 'D':
                return Double.longBitsToDouble(primitives[index]);
            default:
                return (int) primitives[index]; // I, Z
        }
    }

    public int getAsInt(int index) {
        byte t = index < argTypes.length ? argTypes[index] : 0;
        if (t == 0) return ((Number) refs[index]).intValue();
        switch (t) {
            case 'J':
                return (int) primitives[index];
            case 'F':
                return (int) Float.intBitsToFloat((int) primitives[index]);
            case 'D':
                return (int) Double.longBitsToDouble(primitives[index]);
            default:
                return (int) primitives[index]; // I, Z
        }
    }

    public long getAsLong(int index) {
        byte t = index < argTypes.length ? argTypes[index] : 0;
        if (t == 0) return ((Number) refs[index]).longValue();
        switch (t) {
            case 'I':
                return (int) primitives[index]; // sign-extend
            case 'F':
                return (long) Float.intBitsToFloat((int) primitives[index]);
            case 'D':
                return (long) Double.longBitsToDouble(primitives[index]);
            default:
                return primitives[index]; // J, Z
        }
    }

    public Object getArgBoxed(int index) {
        byte t = index < argTypes.length ? argTypes[index] : 0;
        if (t == 0) return refs[index];
        switch (t) {
            case 'I':
                return (int) primitives[index];
            case 'J':
                return primitives[index];
            case 'F':
                return Float.intBitsToFloat((int) primitives[index]);
            case 'D':
                return Double.longBitsToDouble(primitives[index]);
            case 'Z':
                return primitives[index] != 0;
        }
        return refs[index];
    }

    // ====================== 返回值写入 ======================

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

    // ====================== 返回值读取 ======================

    public long getReturnPrimitive() {
        return returnPrimitive;
    }

    public Object getReturnRef() {
        return returnRef;
    }

    public Type getReturnType() {
        return returnType;
    }

    // ====================== 通用 ======================

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

    @Nullable
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

    // ====================== 内部方法 ======================

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
        this.environment = environment;
        this.returnPrimitive = 0;
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
        this.refs = argCount > 0 ? new Object[argCount] : EMPTY_REFS;
        this.argumentCount = argCount;
        this.environment = environment;
        this.returnPrimitive = 0;
        this.returnRef = null;
        this.returnType = null;
        this.interpreter = null;
    }

    /**
     * 从池中分离，close() 变为 no-op（用于 async 转移所有权）
     */
    public void detachFromPool() {
        this.pool = null;
    }

    @SuppressWarnings("DataFlowIssue")
    void clearForPooling() {
        this.refs = EMPTY_REFS;
        this.argTypes = EMPTY_ARG_TYPES;
        this.argumentCount = 0;
        this.target = null;
        this.environment = null;
        this.returnPrimitive = 0;
        this.returnRef = null;
        this.returnType = null;
        this.interpreter = null;
    }

    private void ensurePrimitivesCapacity(int index) {
        if (primitives.length <= index) {
            int newCap = Math.max(INITIAL_CAPACITY, index + 1);
            long[] newArr = new long[newCap];
            System.arraycopy(primitives, 0, newArr, 0, primitives.length);
            primitives = newArr;
        }
    }

    private void ensureRefsCapacity(int index) {
        if (refs.length <= index) {
            int newCap = Math.max(INITIAL_CAPACITY, index + 1);
            Object[] newArr = new Object[newCap];
            System.arraycopy(refs, 0, newArr, 0, refs.length);
            refs = newArr;
            if (argumentCount < newCap) {
                argumentCount = newCap;
            }
        }
    }

    private void ensureArgTypesCapacity(int index) {
        if (argTypes.length <= index) {
            int newCap = Math.max(INITIAL_CAPACITY, index + 1);
            byte[] newArr = new byte[newCap];
            System.arraycopy(argTypes, 0, newArr, 0, argTypes.length);
            argTypes = newArr;
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
        if (pool != null) {
            pool.release(this);
        }
    }
}
