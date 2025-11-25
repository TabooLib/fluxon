package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * 列表表达式
 */
public class ListExpression implements Expression {
    private final List<ParseResult> elements;
    private final boolean immutable;

    public ListExpression(List<ParseResult> elements) {
        this(elements, false);
    }

    public ListExpression(List<ParseResult> elements, boolean immutable) {
        this.elements = elements;
        this.immutable = immutable;
    }

    public List<ParseResult> getElements() {
        return elements;
    }

    public boolean isImmutable() {
        return immutable;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.LIST;
    }

    @Override
    public String toString() {
        return "ListLiteral(" + elements + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(elements.get(i).toPseudoCode());
        }
        sb.append("]");
        if (immutable) {
            sb.append("!");
        }
        return sb.toString();
    }
}
