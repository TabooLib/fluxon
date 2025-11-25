package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.statement.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockParser {

    /**
     * 解析代码块
     * 代码块里包含多个语句（Statement）
     *
     * @return 代码块解析结果
     */
    public static Block parse(Parser parser) {
        return (Block) Trampoline.run(parse(parser, Trampoline::done));
    }

    /**
     * CPS 形式的代码块解析，便于与表达式 trampoline 串联，避免深度嵌套时的栈消耗。
     */
    public static Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        List<ParseResult> statements = new ArrayList<>();
        return Trampoline.more(() -> parseBody(parser, statements, continuation));
    }

    private static Trampoline<ParseResult> parseBody(Parser parser, List<ParseResult> statements, Trampoline.Continuation<ParseResult> continuation) {
        if (parser.check(TokenType.RIGHT_BRACE) || parser.isAtEnd()) {
            parser.consume(TokenType.RIGHT_BRACE, "Expected '}' after block");
            return continuation.apply(new Block(null, statements.toArray(new ParseResult[0])));
        }
        return StatementParser.parseSub(parser, stmt -> {
            statements.add(stmt);
            return parseBody(parser, statements, continuation);
        });
    }
}
