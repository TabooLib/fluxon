package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PrattParser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.TernaryExpression;

/**
 * 三元运算符 (? :)
 * <p>
 * 绑定力: 30，右结合
 */
public class TernaryInfixOperator implements InfixOperator {

    @Override
    public int bindingPower() {
        return 30;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.QUESTION);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult condition, Token question,
                                         Trampoline.Continuation<ParseResult> continuation) {
        // 右结合：使用相同绑定力
        return PrattParser.parseExpression(parser, bindingPower(), trueExpr -> {
            parser.consume(TokenType.COLON, "Expected ':' after ternary true expression");
            return PrattParser.parseExpression(parser, bindingPower(), falseExpr ->
                    continuation.apply(parser.attachSource(new TernaryExpression(condition, trueExpr, falseExpr), question)));
        });
    }
}
