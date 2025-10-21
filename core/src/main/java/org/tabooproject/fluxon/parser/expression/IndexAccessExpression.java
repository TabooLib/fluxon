package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.List;

/**
 * 索引访问表达式 target[index1, index2, ...]
 *
 * @author sky
 */
public class IndexAccessExpression implements Expression {

    private final ParseResult target;
    private final List<ParseResult> indices;
    private final int position;

    public IndexAccessExpression(ParseResult target, List<ParseResult> indices, int position) {
        this.target = target;
        this.indices = indices;
        this.position = position;
    }

    public ParseResult getTarget() {
        return target;
    }

    public List<ParseResult> getIndices() {
        return indices;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.INDEX_ACCESS;
    }

    @Override
    public String toString() {
        return "IndexAccess(" + target + ", indices: " + indices + ", position: " + position + ")";
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(target.toPseudoCode()).append("[");
        for (int i = 0; i < indices.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(indices.get(i).toPseudoCode());
        }
        sb.append("]");
        return sb.toString();
    }
}
