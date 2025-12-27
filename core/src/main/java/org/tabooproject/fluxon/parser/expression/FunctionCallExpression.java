package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.ParseResult;

import java.util.Arrays;

/**
 * 函数调用
 */
public class FunctionCallExpression extends Expression {

    private final String functionName;
    private final ParseResult[] arguments;

    @Nullable
    private FunctionPosition position;
    @Nullable
    private ExtensionFunctionPosition extensionPosition;

    public FunctionCallExpression(String functionName, ParseResult[] arguments, @Nullable FunctionPosition pos1, @Nullable ExtensionFunctionPosition pos2) {
        super(ExpressionType.FUNCTION_CALL);
        this.functionName = functionName;
        this.arguments = arguments;
        this.position = pos1;
        this.extensionPosition = pos2;
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
     * 获取函数解析时预测的位置索引
     */
    public int getPositionIndex() {
        return position != null ? position.getIndex() : -1;
    }

    /**
     * 获取扩展函数解析时预测的位置
     */
    @Nullable
    public ExtensionFunctionPosition getExtensionPosition() {
        return extensionPosition;
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
