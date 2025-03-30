package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.RangeExpression;

import java.util.ArrayList;
import java.util.List;

public class RangeEvaluator extends ExpressionEvaluator<RangeExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.RANGE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, RangeExpression result) {
        // 获取开始值和结束值
        Object start = interpreter.evaluate(result.getStart());
        Object end = interpreter.evaluate(result.getEnd());

        // 检查开始值和结束值是否为数字
        checkNumberOperands(start, end);

        // 转换为整数
        int startInt = ((Number) start).intValue();
        int endInt = ((Number) end).intValue();

        // 检查范围是否为包含上界类型
        boolean isInclusive = result.isInclusive();
        if (!isInclusive) {
            endInt--;
        }

        // 创建范围结果列表
        List<Integer> rangeList = new ArrayList<>();
        // 支持正向和反向范围
        if (startInt <= endInt) {
            // 正向范围
            for (int i = startInt; i <= endInt; i++) {
                rangeList.add(i);
            }
        } else {
            // 反向范围
            for (int i = startInt; i >= endInt; i--) {
                rangeList.add(i);
            }
        }
        return rangeList;
    }
}
