package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.AwaitExpression;

/**
 * await 前缀运算符
 * <p>
 * 优先级: 50
 * <p>
 * 右绑定力: 105（低于 :: 的 110，使得 await foo::bar() 解析为 await(foo::bar())）
 */
public class AwaitPrefixOperator implements PrefixOperator {

    /**
     * 右绑定力：决定右侧表达式的结合强度
     */
    private static final int RIGHT_BINDING_POWER = 105;

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.AWAIT);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token awaitToken = parser.consume();
        // 使用 parseExpression 并指定绑定力，让高优先级的中缀运算符（如 ::）能被正确解析
        return PrattParser.parseExpression(parser, RIGHT_BINDING_POWER, expr ->
                continuation.apply(parser.attachSource(new AwaitExpression(expr), awaitToken)));
    }
}
