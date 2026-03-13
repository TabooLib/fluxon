package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.LogicalExpression;

/**
 * 比较运算符 (>, >=, <, <=)
 * <p>
 * 绑定力: 80，左结合
 */
public class ComparisonInfixOperator implements InfixOperator {

    private static final TokenType[] OPERATORS = {
            TokenType.GREATER,
            TokenType.GREATER_EQUAL,
            TokenType.LESS,
            TokenType.LESS_EQUAL
    };

    @Override
    public int bindingPower() {
        return 80;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.checkAny(OPERATORS);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        return PrattParser.parseExpression(parser, bindingPower() + 1, right ->
                buildChain(parser, left, operator, right, continuation));
    }

    /**
     * 递归构建链式比较表达式
     * 将 a < b < c 脱糖为 a < b && b < c
     */
    private Trampoline<ParseResult> buildChain(Parser parser, ParseResult left, Token op, ParseResult right, Trampoline.Continuation<ParseResult> continuation) {
        ParseResult comparison = parser.attachSource(new BinaryExpression(left, op, right), op);
        // 检查下一个 token 是否也是比较运算符，构成链式比较
        if (parser.checkAny(OPERATORS)) {
            Token nextOp = parser.consume();
            return PrattParser.parseExpression(parser, bindingPower() + 1, right2 ->
                    buildChain(parser, right, nextOp, right2, chained -> {
                        // 合成 AND: (left op right) && (right nextOp right2 ...)
                        Token andToken = new Token(TokenType.AND, "&&", op.getLine(), op.getColumn());
                        ParseResult combined = new LogicalExpression(comparison, andToken, chained);
                        return continuation.apply(combined);
                    }));
        }
        return continuation.apply(comparison);
    }
}
