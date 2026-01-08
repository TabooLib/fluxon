package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;

/**
 * 相等性运算符 (==, !=, ===, !==)
 * <p>
 * 绑定力: 70，左结合
 */
public class EqualityInfixOperator implements InfixOperator {

    private static final TokenType[] OPERATORS = {
            TokenType.EQUAL,
            TokenType.NOT_EQUAL,
            TokenType.IDENTICAL,
            TokenType.NOT_IDENTICAL
    };

    @Override
    public int bindingPower() {
        return 70;
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
