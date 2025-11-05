package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.ParseResult;

import java.util.Arrays;

/**
 * 函数调用
 */
public class FunctionCallExpression implements Expression {
    private final String callee;
    private final ParseResult[] arguments;

    @Nullable
    private final FunctionPosition position;
    @Nullable
    private final ExtensionFunctionPosition extensionPosition;

    public FunctionCallExpression(String callee, ParseResult[] arguments, @Nullable FunctionPosition pos1, @Nullable ExtensionFunctionPosition pos2) {
        this.callee = callee;
        this.arguments = arguments;
        this.position = pos1;
        this.extensionPosition = pos2;
    }

    public String getCallee() {
        return callee;
    }

    public ParseResult[] getArguments() {
        return arguments;
    }

    @Nullable
    public FunctionPosition getPosition() {
        return position;
    }

    @Nullable
    public ExtensionFunctionPosition getExtensionPosition() {
        return extensionPosition;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public String toString() {
        return "Call(" + callee + ", " + Arrays.toString(arguments) + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(callee).append("(");
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
