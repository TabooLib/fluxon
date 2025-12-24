package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PrattParser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.UnaryExpression;

/**
 * 一元前缀运算符 (!, -)
 * <p>
 * 优先级: 50
 */
public class UnaryPrefixOperator implements PrefixOperator {

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public boolean matches(Parser parser) {
        TokenType type = parser.peek().getType();
        return type == TokenType.NOT || type == TokenType.MINUS;
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token operator = parser.consume();
        // 递归解析前缀表达式
        return PrattParser.parsePrefix(parser, expr ->
                continuation.apply(parser.attachSource(new UnaryExpression(operator, expr), operator)));
    }
}
