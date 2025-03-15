package org.tabooproject.fluxon.parser.parselet;

import org.tabooproject.fluxon.parser.PrattParser;

/**
 * 中缀解析函数基类
 */
public abstract class BaseInfixParselet implements InfixParselet {
    private int precedence;

    @Override
    public int getPrecedence() {
        return precedence;
    }

    @Override
    public void setPrecedence(int precedence) {
        this.precedence = precedence;
    }
}
