package org.tabooproject.fluxon.parser.parselet;

import org.tabooproject.fluxon.ast.expression.Expr;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.PrattParser;

/**
 * 空安全访问解析器
 */
public class SafeAccessParselet extends BaseInfixParselet {
    private final PrattParser prattParser;

    public SafeAccessParselet(PrattParser prattParser) {
        this.prattParser = prattParser;
    }

    @Override
    public Expr parse(Expr left, Token token) {
        return prattParser.parseSafeAccess(left, token);
    }
}
