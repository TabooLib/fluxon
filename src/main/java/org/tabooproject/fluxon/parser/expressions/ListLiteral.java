package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * 列表字面量
 */
public class ListLiteral implements Expression {
    private final List<ParseResult> elements;

    public ListLiteral(List<ParseResult> elements) {
        this.elements = elements;
    }

    public List<ParseResult> getElements() {
        return elements;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.LIST_LITERAL;
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
        return sb.toString();
    }
}
