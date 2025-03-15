package org.tabooproject.fluxon.parser.parselet;

import org.tabooproject.fluxon.ast.expression.Expr;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.PrattParser;

/**
 * Elvis 操作符解析器
 */
public class ElvisParselet extends BaseInfixParselet {
    private final PrattParser prattParser;

    public ElvisParselet(PrattParser prattParser) {
        this.prattParser = prattParser;
    }

    @Override
    public Expr parse(Expr left, Token token) {
        return prattParser.parseElvis(left, token);
    }
}
