package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.RangeExpression;

/**
 * 范围运算符 (.., ..<)
 * <p>
 * 绑定力: 60，非结合（不允许链式）
 */
public class RangeInfixOperator implements InfixOperator {

    @Override
    public int bindingPower() {
        return 60;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.RANGE) || parser.check(TokenType.RANGE_EXCLUSIVE);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        boolean inclusive = operator.getType() == TokenType.RANGE;
        // 非结合：使用更高的绑定力防止链式
        return PrattParser.parseExpression(parser, bindingPower() + 1, right ->
                continuation.apply(parser.attachSource(new RangeExpression(left, right, inclusive), operator)));
    }
}
