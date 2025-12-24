package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.LogicalExpression;

/**
 * 逻辑或运算符 (||)
 * <p>
 * 绑定力: 40，左结合
 */
public class LogicalOrInfixOperator implements InfixOperator {

    @Override
    public int bindingPower() {
        return 40;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.OR);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        return PrattParser.parseExpression(parser, bindingPower() + 1, right ->
                continuation.apply(parser.attachSource(new LogicalExpression(left, operator, right), operator)));
    }
}
