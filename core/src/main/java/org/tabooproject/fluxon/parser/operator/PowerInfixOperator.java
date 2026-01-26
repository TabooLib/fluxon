package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;

/**
 * 幂运算符 (^)
 * <p>
 * 绑定力: 105，右结合
 *
 * @author sky
 */
public class PowerInfixOperator implements InfixOperator {

    @Override
    public int bindingPower() {
        return 105;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.POWER);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        // 右结合：右侧使用 bindingPower()（不加 1）
        return PrattParser.parseExpression(parser, bindingPower(), right ->
                continuation.apply(parser.attachSource(new BinaryExpression(left, operator, right), operator)));
    }
}
