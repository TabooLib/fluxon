package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.MapExpression;

import java.util.HashMap;
import java.util.Map;

public class MapEvaluator extends ExpressionEvaluator<MapExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.MAP;
    }

    @Override
    public Object evaluate(Interpreter interpreter, MapExpression result) {
        Map<Object, Object> entries = new HashMap<>();
        for (MapExpression.MapEntry entry : result.getEntries()) {
            Object key = interpreter.evaluate(entry.getKey());
            Object value = interpreter.evaluate(entry.getValue());
            entries.put(key, value);
        }
        return entries;
    }
}
