package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PrattParser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.AwaitExpression;

/**
 * await 前缀运算符
 * <p>
 * 优先级: 50
 */
public class AwaitPrefixOperator implements PrefixOperator {

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.AWAIT);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token awaitToken = parser.consume();
        return PrattParser.parsePrefix(parser, expr ->
                continuation.apply(parser.attachSource(new AwaitExpression(expr), awaitToken)));
    }
}
