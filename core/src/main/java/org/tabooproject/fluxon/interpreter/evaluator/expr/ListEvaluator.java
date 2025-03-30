package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ListLiteral;

import java.util.ArrayList;
import java.util.List;

public class ListEvaluator extends ExpressionEvaluator<ListLiteral> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.LIST_LITERAL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ListLiteral result) {
        List<Object> elements = new ArrayList<>();
        for (ParseResult element : result.getElements()) {
            elements.add(interpreter.evaluate(element));
        }
        return elements;
    }
}
