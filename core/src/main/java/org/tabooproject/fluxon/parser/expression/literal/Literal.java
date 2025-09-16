package org.tabooproject.fluxon.parser.expression.literal;

import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;

/**
 * 字面量表达式基类
 */
public abstract class Literal implements Expression {

    // 字面量基类，具体实现由子类提供
    @Override
    public abstract ExpressionType getExpressionType();

    // 获取字面量值
    public abstract Object getSourceValue();
}
