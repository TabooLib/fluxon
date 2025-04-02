package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.expression.Assignment;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Environment;

import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class AssignmentEvaluator extends ExpressionEvaluator<Assignment> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ASSIGNMENT;
    }

    @Override
    public Object evaluate(Interpreter interpreter, Assignment result) {
        Object value = interpreter.evaluate(result.getValue());
        Environment environment = interpreter.getEnvironment();
        // 根据赋值操作符类型处理赋值
        if (result.getOperator().getType() == TokenType.ASSIGN) {
            environment.assign(result.getName(), value);
        } else {
            // 处理复合赋值
            Object current = environment.get(result.getName());
            if (!(current instanceof Number) || !(value instanceof Number)) {
                throw new RuntimeException("Operands for compound assignment must be numbers.");
            }
            // 根据操作符类型进行不同的复合赋值操作
            switch (result.getOperator().getType()) {
                case PLUS_ASSIGN:
                    value = addNumbers((Number) current, (Number) value);
                    break;
                case MINUS_ASSIGN:
                    value = subtractNumbers((Number) current, (Number) value);
                    break;
                case MULTIPLY_ASSIGN:
                    value = multiplyNumbers((Number) current, (Number) value);
                    break;
                case DIVIDE_ASSIGN:
                    value = divideNumbers((Number) current, (Number) value);
                    break;
                case MODULO_ASSIGN:
                    value = moduloNumbers((Number) current, (Number) value);
                    break;
                default:
                    throw new RuntimeException("Unknown compound assignment operator: " + result.getOperator().getType());
            }
            environment.assign(result.getName(), value);
        }
        return value;
    }
}
