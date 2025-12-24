package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;

/**
 * 标识符语法宏
 * <p>
 * 匹配普通标识符，优先级最低，作为兜底处理
 */
public class IdentifierSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.IDENTIFIER);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token token = parser.consume();
        return Trampoline.more(() -> continuation.apply(parser.attachSource(new Identifier(token.getLexeme()), token)));
    }

    @Override
    public int priority() {
        return 10;
    }
}
