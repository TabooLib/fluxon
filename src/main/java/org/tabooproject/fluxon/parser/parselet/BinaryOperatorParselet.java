package org.tabooproject.fluxon.parser.parselet;

import org.tabooproject.fluxon.ast.expression.Expr;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.PrattParser;

/**
 * 二元操作符解析器
 */
public class BinaryOperatorParselet extends BaseInfixParselet {
    private final PrattParser prattParser;

    public BinaryOperatorParselet(PrattParser prattParser) {
        this.prattParser = prattParser;
    }

    @Override
    public Expr parse(Expr left, Token token) {
        return prattParser.parseBinary(left, token);
    }
}
