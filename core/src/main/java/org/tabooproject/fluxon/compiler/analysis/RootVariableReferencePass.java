package org.tabooproject.fluxon.compiler.analysis;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;

import java.util.List;

/**
 * 收集根层级变量读取（{@link ReferenceExpression#getPosition()} &lt; 0）。
 */
public final class RootVariableReferencePass implements ScriptAnalysisPass {

    @Override
    public void run(List<ParseResult> results, ScriptAnalysisBuilder builder) {
        if (results == null) {
            return;
        }
        for (ParseResult result : results) {
            visit(result, builder);
        }
    }

    private void visit(ParseResult node, ScriptAnalysisBuilder builder) {
        if (node == null) {
            return;
        }
        if (node instanceof ExpressionStatement) {
            visit(((ExpressionStatement) node).getExpression(), builder);
            return;
        }
        if (node instanceof ReferenceExpression) {
            ReferenceExpression reference = (ReferenceExpression) node;
            if (reference.getPosition() < 0) {
                builder.mergeString(ScriptAnalysisKeys.REFERENCED_ROOT_VARIABLES, reference.getIdentifier().getValue());
            }
            return;
        }
        node.forEachChild(child -> visit(child, builder));
    }
}