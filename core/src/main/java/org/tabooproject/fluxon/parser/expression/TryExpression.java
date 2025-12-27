package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.Block;

/**
 * try 表达式
 */
public class TryExpression extends Expression {
    private final ParseResult body;
    @Nullable
    private final String catchName;
    private final int position;
    @Nullable
    private final ParseResult catchBody;
    @Nullable
    private final ParseResult finallyBody;

    public TryExpression(ParseResult body, @Nullable String catchName, int position, @Nullable ParseResult catchBody, @Nullable ParseResult finallyBody) {
        super(ExpressionType.TRY);
        this.body = body;
        this.catchName = catchName;
        this.position = position;
        this.catchBody = catchBody;
        this.finallyBody = finallyBody;
    }

    public ParseResult getBody() {
        return body;
    }

    @Nullable
    public String getCatchName() {
        return catchName;
    }

    public int getPosition() {
        return position;
    }

    @Nullable
    public ParseResult getCatchBody() {
        return catchBody;
    }

    @Nullable
    public ParseResult getFinallyBody() {
        return finallyBody;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.TRY;
    }

    @Override
    public String toString() {
        return "Try(" + body + ", " + catchName + ", " + catchBody + ", " + finallyBody + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("try ");

        if (body instanceof Block) {
            sb.append(body.toPseudoCode());
        } else {
            sb.append("{ ").append(body.toPseudoCode()).append(" }");
        }

        if (catchBody != null) {
            sb.append("catch");
            if (catchName != null) {
                sb.append(" (").append(catchName).append(") ");
            } else {
                sb.append(" ");
            }
            if (catchBody instanceof Block) {
                sb.append(catchBody.toPseudoCode());
            } else {
                sb.append("{ ").append(catchBody.toPseudoCode()).append(" }");
            }
        }
        if (finallyBody != null) {
            sb.append("finally ");
            if (finallyBody instanceof Block) {
                sb.append(finallyBody.toPseudoCode());
            } else {
                sb.append("{ ").append(finallyBody.toPseudoCode()).append(" }");
            }
        }
        return sb.toString();
    }
}
