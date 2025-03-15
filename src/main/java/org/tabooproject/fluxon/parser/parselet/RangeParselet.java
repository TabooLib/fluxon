package org.tabooproject.fluxon.parser.parselet;

import org.tabooproject.fluxon.ast.expression.Expr;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.PrattParser;

/**
 * 范围操作符解析器
 */
public class RangeParselet extends BaseInfixParselet {
    private final PrattParser prattParser;

    public RangeParselet(PrattParser prattParser) {
        this.prattParser = prattParser;
    }

    @Override
    public Expr parse(Expr left, Token token) {
        return prattParser.parseRange(left, token);
    }
}
