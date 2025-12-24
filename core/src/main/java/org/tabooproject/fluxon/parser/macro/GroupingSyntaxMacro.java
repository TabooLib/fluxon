package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.GroupingExpression;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

/**
 * 分组表达式语法宏
 * <p>
 * 匹配 (...) 括号表达式
 */
public class GroupingSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.LEFT_PAREN);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token leftParen = parser.consume();
        return ExpressionParser.parse(parser, expr -> {
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return continuation.apply(parser.attachSource(new GroupingExpression(expr), leftParen));
        });
    }

    @Override
    public int priority() {
        return 50;
    }
}
