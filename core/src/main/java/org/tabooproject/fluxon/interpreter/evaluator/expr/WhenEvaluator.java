package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.WhenExpression;

import java.util.List;
import java.util.Map;

import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class WhenEvaluator extends ExpressionEvaluator<WhenExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.WHEN;
    }

    @Override
    public Object evaluate(Interpreter interpreter, WhenExpression result) {
        // 获取并评估主题对象（如果有）
        Object subject = null;
        if (result.getSubject() != null) {
            subject = interpreter.evaluate(result.getSubject());
        }

        // 遍历所有分支
        for (WhenExpression.WhenBranch branch : result.getBranches()) {
            // 如果是 else 分支（没有条件），直接返回其结果
            if (branch.getCondition() == null) {
                return interpreter.evaluate(branch.getResult());
            }

            // 评估分支条件
            Object condition = interpreter.evaluate(branch.getCondition());
            boolean matches = false;

            // 根据匹配类型进行判断
            switch (branch.getMatchType()) {
                case EQUAL:
                    // 如果有主题，判断主题和条件是否相等
                    if (subject != null) {
                        matches = isEqual(subject, condition);
                    } else {
                        // 没有主题时，直接判断条件是否为真
                        matches = isTrue(condition);
                    }
                    break;
                // 判断主题是否包含在条件中
                case CONTAINS:
                    if (subject != null && condition != null) {
                        if (condition instanceof List) {
                            matches = ((List<?>) condition).contains(subject);
                        } else if (condition instanceof Map) {
                            matches = ((Map<?, ?>) condition).containsKey(subject);
                        } else if (condition instanceof String && subject instanceof String) {
                            matches = ((String) condition).contains((String) subject);
                        }
                    }
                    break;
                // 判断主题是否不包含在条件中
                case NOT_CONTAINS:
                    if (subject != null && condition != null) {
                        if (condition instanceof List) {
                            matches = !((List<?>) condition).contains(subject);
                        } else if (condition instanceof Map) {
                            matches = !((Map<?, ?>) condition).containsKey(subject);
                        } else if (condition instanceof String && subject instanceof String) {
                            matches = !((String) condition).contains((String) subject);
                        }
                    }
                    break;
            }
            // 如果匹配成功，执行对应分支的结果
            if (matches) {
                return interpreter.evaluate(branch.getResult());
            }
        }
        // 如果没有匹配的分支，返回 null
        return null;
    }
}
