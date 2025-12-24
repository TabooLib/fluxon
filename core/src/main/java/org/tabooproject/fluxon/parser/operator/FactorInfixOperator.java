package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PrattParser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;

/**
 * 因子运算符 (*, /, %)
 * <p>
 * 绑定力: 100，左结合
 */
public class FactorInfixOperator implements InfixOperator {

    private static final TokenType[] OPERATORS = {
            TokenType.MULTIPLY,
            TokenType.DIVIDE,
            TokenType.MODULO
    };

    @Override
    public int bindingPower() {
        return 100;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.checkAny(OPERATORS);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator,
                                         Trampoline.Continuation<ParseResult> continuation) {
        // 左结合：右侧使用 bindingPower + 1
        return PrattParser.parseExpression(parser, bindingPower() + 1, right ->
                continuation.apply(parser.attachSource(new BinaryExpression(left, operator, right), operator)));
    }
}
