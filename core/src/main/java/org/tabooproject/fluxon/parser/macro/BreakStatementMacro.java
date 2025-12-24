package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.StatementMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.statement.BreakStatement;

/**
 * Break 语句宏
 * <p>
 * 处理 break 语句，仅支持子语句（循环内部）。
 */
public class BreakStatementMacro implements StatementMacro {

    @Override
    public boolean matchesSub(Parser parser) {
        return parser.check(TokenType.BREAK);
    }

    @Override
    public Trampoline<ParseResult> parseSub(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token breakToken = parser.consume(TokenType.BREAK, "Expected 'break'");
        if (!parser.getSymbolEnvironment().isBreakable()) {
            throw new RuntimeException("Break outside of loop");
        }
        parser.match(TokenType.SEMICOLON);
        return continuation.apply(parser.attachSource(new BreakStatement(), breakToken));
    }

    @Override
    public int priority() {
        return 100;
    }
}
