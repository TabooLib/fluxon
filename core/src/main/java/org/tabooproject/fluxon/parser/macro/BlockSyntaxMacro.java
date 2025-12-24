package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.type.BlockParser;

/**
 * 块表达式语法宏
 * <p>
 * 匹配 { ... } 块表达式
 */
public class BlockSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.LEFT_BRACE);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token leftBrace = parser.consume();
        return BlockParser.parse(parser, expr -> continuation.apply(parser.attachSource(expr, leftBrace)));
    }

    @Override
    public int priority() {
        return 80;
    }
}
