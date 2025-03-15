package org.tabooproject.fluxon.parser.parselet;

import org.tabooproject.fluxon.ast.expression.Expr;
import org.tabooproject.fluxon.lexer.Token;

/**
 * 前缀解析函数接口
 */
@FunctionalInterface
public interface PrefixParselet {
    Expr parse(Token token);
}
