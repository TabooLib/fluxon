package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * 函数调用
 */
public class FunctionCall implements Expression {
    private final ParseResult callee;
    private final List<ParseResult> arguments;

    public FunctionCall(ParseResult callee, List<ParseResult> arguments) {
        this.callee = callee;
        this.arguments = arguments;
    }

    public ParseResult getCallee() {
        return callee;
    }

    public List<ParseResult> getArguments() {
        return arguments;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public String toString() {
        return "Call(" + callee + ", " + arguments + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(callee.toPseudoCode()).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arguments.get(i).toPseudoCode());
        }
        sb.append(")");
        return sb.toString();
    }
}
