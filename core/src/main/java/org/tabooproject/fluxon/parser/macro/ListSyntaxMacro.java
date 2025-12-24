package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.type.ListParser;

/**
 * 列表/Map 字面量语法宏
 * <p>
 * 匹配 [...] 列表或 [key: value, ...] Map 字面量
 */
public class ListSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.LEFT_BRACKET);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token token = parser.consume();
        return Trampoline.more(() -> continuation.apply(parser.attachSource(ListParser.parse(parser), token)));
    }

    @Override
    public int priority() {
        return 80;
    }
}
