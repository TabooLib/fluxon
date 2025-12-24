package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;

/**
 * 项运算符 (+, -)
 * <p>
 * 绑定力: 90，左结合
 */
public class TermInfixOperator implements InfixOperator {

    private static final TokenType[] OPERATORS = {
            TokenType.PLUS,
            TokenType.MINUS
    };

    @Override
    public int bindingPower() {
        return 90;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.checkAny(OPERATORS);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        return PrattParser.parseExpression(parser, bindingPower() + 1, right ->
                continuation.apply(parser.attachSource(new BinaryExpression(left, operator, right), operator)));
    }
}
