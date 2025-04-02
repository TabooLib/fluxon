package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.LogicalExpression;

import static org.tabooproject.fluxon.runtime.stdlib.Operations.isTrue;

public class LogicalEvaluator extends ExpressionEvaluator<LogicalExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.LOGICAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, LogicalExpression result) {
        Object left = interpreter.evaluate(result.getLeft());
        // 逻辑或
        if (result.getOperator().getType() == TokenType.OR) {
            if (isTrue(left)) return left;
        } else {
            if (!isTrue(left)) return left;
        }
        return interpreter.evaluate(result.getRight());
    }
}
