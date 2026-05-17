package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.ExtensionDispatchTable;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.OverloadSet;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Arrays;

/**
 * 函数调用
 */
public class FunctionCallExpression extends Expression {

    /**
     * 运行时函数解析缓存（不可变记录）
     * volatile 写入保证 happens-before：其他线程读到引用时，字段值一定已完整写入
     */
    public static final class CachedResolution {

        public final Function function;
        public final Type[] expectedTypes;
        public final Class<?> guardClass;

        public CachedResolution(Function function, Type[] expectedTypes, Class<?> guardClass) {
            this.function = function;
            this.expectedTypes = expectedTypes;
            this.guardClass = guardClass;
        }
    }

    private final String functionName;
    private final ParseResult[] arguments;
    @Nullable
    private FunctionPosition position;
    @Nullable
    private ExtensionFunctionPosition extensionPosition;
    // 是否为 obj::foo() 右侧的直接上下文调用目标
    private final boolean directContextCall;
    // 类型分析后解析的具体重载索引
    private int resolvedPositionIndex = -1;
    // 类型分析后解析的具体扩展函数（基于 target 类型）
    @Nullable
    private Function resolvedExtensionFunction;
    // 预解析扩展函数的派发表索引（用于编译期优化）
    private int resolvedDispatchTableIndex = -1;
    // 预解析扩展函数的目标类型（用于编译期优化）
    @Nullable
    private Class<?> resolvedTargetClass;
    // 预解析扩展函数在 OverloadSet 中的索引
    private int resolvedOverloadIndex = -1;
    // 运行时函数解析缓存（volatile 保证跨线程可见性）
    public volatile CachedResolution cachedResolution;

    public FunctionCallExpression(String functionName, ParseResult[] arguments, @Nullable FunctionPosition pos1, @Nullable ExtensionFunctionPosition pos2) {
        this(functionName, arguments, pos1, pos2, false);
    }

    public FunctionCallExpression(String functionName, ParseResult[] arguments, @Nullable FunctionPosition pos1, @Nullable ExtensionFunctionPosition pos2, boolean directContextCall) {
        super(ExpressionType.FUNCTION_CALL);
        this.functionName = functionName;
        this.arguments = arguments;
        this.position = pos1;
        this.extensionPosition = pos2;
        this.directContextCall = directContextCall;
    }

    /**
     * 设置函数位置（用于延迟解析）
     */
    public void setPosition(@Nullable FunctionPosition position) {
        this.position = position;
    }

    /**
     * 设置扩展函数位置（用于延迟解析）
     */
    public void setExtensionPosition(@Nullable ExtensionFunctionPosition extensionPosition) {
        this.extensionPosition = extensionPosition;
    }

    /**
     * 设置类型分析后解析的具体重载索引
     */
    public void setResolvedPositionIndex(int index) {
        this.resolvedPositionIndex = index;
    }

    /**
     * 设置类型分析后解析的具体扩展函数
     */
    public void setResolvedExtensionFunction(@Nullable Function function) {
        this.resolvedExtensionFunction = function;
    }

    /**
     * 设置预解析扩展函数的元信息（用于编译期优化）
     */
    public void setResolvedExtensionInfo(int dispatchTableIndex, @Nullable Class<?> targetClass, int overloadIndex) {
        this.resolvedDispatchTableIndex = dispatchTableIndex;
        this.resolvedTargetClass = targetClass;
        this.resolvedOverloadIndex = overloadIndex;
    }

    /**
     * 解析扩展函数并缓存结果
     */
    @Nullable
    public Function resolveExtensionFunction(Class<?> targetClass, Type[] argTypes) {
        if (extensionPosition == null || targetClass == Object.class) {
            return null;
        }
        ExtensionDispatchTable dispatchTable = FluxonRuntime.getInstance().getCachedDispatchTables()[extensionPosition.getIndex()];
        OverloadSet overloadSet = dispatchTable.resolveOverloadSet(targetClass);
        if (overloadSet == null) {
            return null;
        }
        Function resolved = overloadSet.resolve(argTypes);
        if (resolved != null) {
            int overloadIndex = overloadSet.indexOf(resolved);
            this.resolvedExtensionFunction = resolved;
            setResolvedExtensionInfo(extensionPosition.getIndex(), targetClass, overloadIndex);
        }
        return resolved;
    }

    /**
     * 解析函数期望的参数类型
     */
    @Nullable
    public Type[] resolveExpectedParameterTypes(Type[] argTypes) {
        if (position == null) {
            return null;
        }
        Function function = position.resolve(argTypes);
        if (function != null && function.getSignature() != null) {
            return function.getSignature().getParameterTypes();
        }
        return null;
    }

    /**
     * 获取预解析扩展函数的派发表索引
     */
    public int getResolvedDispatchTableIndex() {
        return resolvedDispatchTableIndex;
    }

    /**
     * 获取预解析扩展函数的目标类型
     */
    @Nullable
    public Class<?> getResolvedTargetClass() {
        return resolvedTargetClass;
    }

    /**
     * 获取预解析扩展函数在 OverloadSet 中的索引
     */
    public int getResolvedOverloadIndex() {
        return resolvedOverloadIndex;
    }

    /**
     * 获取类型分析后解析的具体扩展函数
     */
    @Nullable
    public Function getResolvedExtensionFunction() {
        return resolvedExtensionFunction;
    }

    /**
     * 获取函数名
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * 获取参数
     */
    public ParseResult[] getArguments() {
        return arguments;
    }

    /**
     * 获取函数解析时预测的位置
     */
    @Nullable
    public FunctionPosition getPosition() {
        return position;
    }

    /**
     * 获取函数位置索引
     * 优先返回类型分析后的具体重载索引，否则返回基础索引
     */
    public int getPositionIndex() {
        if (resolvedPositionIndex != -1) {
            return resolvedPositionIndex;
        }
        return position != null ? position.getBaseIndex() : -1;
    }

    /**
     * 获取扩展函数解析时预测的位置
     */
    @Nullable
    public ExtensionFunctionPosition getExtensionPosition() {
        return extensionPosition;
    }

    /**
     * 判断是否为上下文调用右侧的直接 callee
     */
    public boolean isDirectContextCall() {
        return directContextCall;
    }

    /**
     * 获取扩展函数解析时预测的位置索引
     */
    public int getExtensionPositionIndex() {
        return extensionPosition != null ? extensionPosition.getIndex() : -1;
    }

    /**
     * 获取表达式具体类型
     */
    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public String toString() {
        return "Call(" + functionName + ", " + Arrays.toString(arguments) + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append("(");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arguments[i].toPseudoCode());
        }
        sb.append(")");
        return sb.toString();
    }
}
