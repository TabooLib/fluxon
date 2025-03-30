package org.tabooproject.fluxon.interpreter.evaluator;

import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;

public abstract class ExpressionEvaluator<T extends Expression> implements Evaluator<T> {

    /**
     * 表达式类型
     */
    abstract public ExpressionType getType();

    /**
     * 检查操作数是否为数字
     *
     * @param operand 操作数
     */
    protected void checkNumberOperand(Object operand) {
        if (operand instanceof Number) return;
        throw new RuntimeException("Operands must be numbers.");
    }

    /**
     * 检查操作数是否都是数字
     *
     * @param left  左操作数
     * @param right 右操作数
     */
    protected void checkNumberOperands(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) return;
        throw new RuntimeException("Operands must be numbers.");
    }

    /**
     * 评估布尔值，判断对象是否为真
     *
     * @param value 要判断的对象
     * @return 布尔值结果
     */
    protected boolean isTrue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }

    /**
     * 判断两个对象是否相等
     *
     * @param a 第一个对象
     * @param b 第二个对象
     * @return 是否相等
     */
    protected boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue()) == 0;
        } else {
            return a.equals(b);
        }
    }
}
