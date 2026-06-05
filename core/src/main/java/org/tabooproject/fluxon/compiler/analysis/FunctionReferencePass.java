package org.tabooproject.fluxon.compiler.analysis;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;

import java.util.List;

/**
 * 收集脚本中出现的函数调用名。
 */
public final class FunctionReferencePass implements ScriptAnalysisPass {

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
        if (node instanceof FunctionCallExpression) {
            FunctionCallExpression call = (FunctionCallExpression) node;
            String name = call.getFunctionName();
            if (name != null && !name.isEmpty()) {
                builder.mergeString(ScriptAnalysisKeys.REFERENCED_FUNCTIONS, name);
            }
        }
        node.forEachChild(child -> visit(child, builder));
    }
}