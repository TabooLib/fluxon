package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.type.LambdaParser;

/**
 * Lambda 表达式语法宏
 * <p>
 * 匹配 | 或 || 开头的 lambda 表达式
 */
public class LambdaSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        TokenType type = parser.peek().getType();
        return type == TokenType.OR || type == TokenType.PIPE;
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        return LambdaParser.parse(parser, continuation);
    }
}
