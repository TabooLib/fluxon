package org.tabooproject.fluxon.parser.parselet;

import org.tabooproject.fluxon.ast.expression.Expr;
import org.tabooproject.fluxon.lexer.Token;

/**
 * 中缀解析函数接口
 */
public interface InfixParselet {
    Expr parse(Expr left, Token token);

    int getPrecedence();

    void setPrecedence(int precedence);
}
