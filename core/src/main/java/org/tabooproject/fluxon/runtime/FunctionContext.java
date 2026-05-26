package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.interpreter.Interpreter;

import java.util.Map;

/**
 * 函数调用上下文
 * 只保留调用状态和对外协议，参数槽与返回槽由专门对象维护。
 *
 * @author sky
 */
public final class FunctionContext<Target> implements AutoCloseable {

    public static final Type TYPE = new Type(FunctionContext.class);

    private final FunctionArgumentFrame arguments;
    private final FunctionReturnSlot returns = new FunctionReturnSlot();
    private Function function;
    private Target target;
    private Environment environment;
    private CaptureFrame captureFrame;
    private FunctionContextPool pool;
    private Interpreter interpreter;
    int stackIndex = -1;
    // 普通 env-free 函数没有捕获槽，局部变量读写可跳过 CaptureCell 分支。
    private boolean hasCapturedLocals;

    /**
     * 池专用构造函数，字段在 reset 时初始化
     */
    FunctionContext(@NotNull FunctionContextPool pool) {
        this.pool = pool;
        this.arguments = new FunctionArgumentFrame();
    }

    /**
     * 栈槽位构造函数，stackIndex 固化到构造期
     */
    FunctionContext(@NotNull FunctionContextPool pool, int capacity, int stackIndex) {
        this.pool = pool;
        this.arguments = new FunctionArgumentFrame(capacity);
        this.stackIndex = stackIndex;
    }

    public int getInt(int index) {
        return arguments.getInt(index);
    }

    public long getLong(int index) {
        return arguments.getLong(index);
    }

    public double getDouble(int index) {
        return arguments.getDouble(index);
    }

    public float getFloat(int index) {
        return arguments.getFloat(index);
    }

    public boolean getBool(int index) {
        return arguments.getBool(index);
    }

    public long getPrimitive(int index) {
        return arguments.getPrimitive(index);
    }

    public Object getRef(int index) {
        return arguments.getRef(index);
    }

    public CaptureCell getCaptureCell(int index) {
        return arguments.getCaptureCell(index);
    }

    public String getString(int index) {
        return arguments.getString(index);
    }

    public void setInt(int index, int v) {
        arguments.setInt(index, v);
    }

    public void setLong(int index, long v) {
        arguments.setLong(index, v);
    }

    public void setDouble(int index, double v) {
        arguments.setDouble(index, v);
    }

    public void setFloat(int index, float v) {
        arguments.setFloat(index, v);
    }

    public void setBool(int index, boolean v) {
        arguments.setBool(index, v);
    }

    public void setRef(int index, Object v) {
        arguments.setRef(index, v);
    }

    public void setArgumentFromBits(int index, Type target, Type source, long bits) {
        arguments.setFromBits(index, target, source, bits);
    }

    public void setArgumentFromObject(int index, Type target, Object value) {
        arguments.setFromObject(index, target, value);
    }

    public boolean isArgPrimitive(int index) {
        return arguments.isArgPrimitive(index);
    }

    public byte getArgType(int index) {
        return arguments.getArgType(index);
    }

    public double getAsDouble(int index) {
        return arguments.getAsDouble(index);
    }

    public int getAsInt(int index) {
        return arguments.getAsInt(index);
    }

    public long getAsLong(int index) {
        return arguments.getAsLong(index);
    }

    public float getAsFloat(int index) {
        return arguments.getAsFloat(index);
    }

    public boolean getAsBoolean(int index) {
        return arguments.getAsBoolean(index);
    }

    public Object getArgBoxed(int index) {
        return arguments.getArgBoxed(index);
    }

    public void checkArgumentType(int index, Class<?> expect) {
        arguments.checkArgumentType(this, index, expect);
    }

    public void setReturnInt(int v) {
        returns.setInt(v);
    }

    public void setReturnLong(long v) {
        returns.setLong(v);
    }

    public void setReturnDouble(double v) {
        returns.setDouble(v);
    }

    public void setReturnFloat(float v) {
        returns.setFloat(v);
    }

    public void setReturnBool(boolean v) {
        returns.setBool(v);
    }

    public void setReturnRef(Object v) {
        returns.setRef(v);
    }

    public long getReturnPrimitive() {
        return returns.getPrimitive();
    }

    public Object getReturnRef() {
        return returns.getRef();
    }

    public Type getReturnType() {
        return returns.getType();
    }

    public boolean hasPrimitiveReturn() {
        return returns.isPrimitive();
    }

    public Object boxReturnPrimitive() {
        return returns.boxPrimitive();
    }

    public int getArgumentCount() {
        return arguments.getArgumentCount();
    }

    /**
     * 热循环复用 context，更新引用参数
     */
    public void updateRefs(Object... args) {
        arguments.updateRefs(args);
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
    public CaptureFrame getCaptureFrame() {
        return captureFrame;
    }

    /**
     * 临时替换调用环境。
     * 捕获型 Lambda 通过包装函数把调用点环境切换为定义时环境，调用结束后恢复。
     */
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
    }

    public void setCaptureFrame(@Nullable CaptureFrame captureFrame) {
        this.captureFrame = captureFrame;
        if (captureFrame != null) {
            hasCapturedLocals = true;
        }
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
            arguments.convertArgs(sig, actualTypes);
        }
    }

    /**
     * 收集实际参数类型
     */
    public Type[] collectArgTypes() {
        return arguments.collectArgTypes();
    }

    @SuppressWarnings("unchecked")
    void reset(
            @NotNull Function function,
            @Nullable Object target,
            @NotNull Object[] refs,
            @NotNull Environment environment) {
        this.function = function;
        this.target = (Target) target;
        this.environment = environment;
        this.captureFrame = null;
        this.hasCapturedLocals = false;
        this.interpreter = null;
        this.returns.clear();
        this.arguments.resetRefs(refs);
    }

    @SuppressWarnings("unchecked")
    void reset(
            @NotNull Function function,
            @Nullable Object target,
            int argCount,
            @NotNull Environment environment) {
        this.function = function;
        this.target = (Target) target;
        this.environment = environment;
        this.captureFrame = null;
        this.hasCapturedLocals = false;
        this.interpreter = null;
        this.returns.clear();
        this.arguments.resetCapacity(argCount);
    }

    /**
     * 脚本执行结束后清理空闲槽引用，避免线程池复用线程时保留上一轮变量图
     */
    void clearIdleReferences() {
        function = null;
        target = null;
        environment = null;
        captureFrame = null;
        hasCapturedLocals = false;
        interpreter = null;
        returns.clear();
        arguments.clearIdleReferences();
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
        arguments.ensureLocalCapacity(count);
    }

    /**
     * 将原始类型参数统一装箱到 refs 数组（env-free 解释器路径使用）
     * 调用后所有参数位置的 argTypes 均为 TYPE_REF，getLocal 可直接读 refs
     *
     * @param paramCount 参数数量
     */
    public void normalizeArgsToRef(int paramCount) {
        arguments.normalizeArgsToRef(paramCount);
    }

    /**
     * 将调用参数绑定到解析期分配的局部 slot。
     * 捕获型 Lambda 的参数 slot 位于父捕获槽之后，不能假定参数从 0 连续开始。
     */
    public void normalizeArgsToParameterSlots(Map<String, Integer> parameters) {
        arguments.normalizeArgsToParameterSlots(parameters);
    }

    /**
     * 获取指定位置的值（env-free 解释器路径使用）
     * 调用 normalizeArgsToRef 后所有位置均为 TYPE_REF，直接读 refs
     *
     * @param index 位置索引
     * @return 值
     */
    public Object getLocal(int index) {
        return arguments.getLocal(captureFrame, hasCapturedLocals, index);
    }

    /**
     * 设置指定位置的值（env-free 解释器路径使用）
     *
     * @param index 位置索引
     * @param value 值
     */
    public void setLocal(int index, Object value) {
        arguments.setLocal(captureFrame, hasCapturedLocals, index, value);
    }

    /**
     * 将指定槽位转换为捕获 cell，供父函数和逃逸 Lambda 共享。
     */
    public void ensureCaptureCell(int index) {
        hasCapturedLocals = true;
        arguments.ensureCaptureCell(index);
    }

    /**
     * 根据当前局部槽位构建 Lambda 捕获帧。
     */
    public CaptureFrame createCaptureFrame(int size) {
        hasCapturedLocals = true;
        return arguments.createCaptureFrame(captureFrame, size);
    }

    @Override
    public String toString() {
        return "FunctionContext{" +
                "function=" + function.getName() +
                ", target=" + target +
                ", arguments=" + arguments.getArgumentCount() +
                '}';
    }

    @Override
    public void close() {
        pool.releaseUnchecked(this);
    }
}
