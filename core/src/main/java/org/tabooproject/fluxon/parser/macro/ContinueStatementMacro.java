package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.StatementMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.statement.ContinueStatement;

/**
 * Continue 语句宏
 * <p>
 * 处理 continue 语句，仅支持子语句（循环内部）。
 */
public class ContinueStatementMacro implements StatementMacro {

    @Override
    public boolean matchesSub(Parser parser) {
        return parser.check(TokenType.CONTINUE);
    }

    @Override
    public Trampoline<ParseResult> parseSub(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token continueToken = parser.consume(TokenType.CONTINUE, "Expected 'continue'");
        if (!parser.getSymbolEnvironment().isContinuable()) {
            throw new RuntimeException("Continue outside of loop");
        }
        parser.match(TokenType.SEMICOLON);
        return continuation.apply(parser.attachSource(new ContinueStatement(), continueToken));
    }

    @Override
    public int priority() {
        return 100;
    }
}
