package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.TryExpression;

public class TryParser {

    /**
     * 解析 try 表达式
     * 语法：try { ... } [catch (var) { ... }] [finally { ... }]
     *
     * @return try 表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        return Trampoline.run(parse(parser, Trampoline::done));
    }

    public static Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        parser.consume(TokenType.TRY, "Expected 'try' before try expression");
        return parseTryBody(parser, tryBody -> parseCatch(parser, tryBody, continuation));
    }

    private static Trampoline<ParseResult> parseTryBody(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        if (parser.match(TokenType.LEFT_BRACE)) {
            return BlockParser.parse(parser, continuation);
        }
        return ExpressionParser.parse(parser, continuation);
    }

    private static Trampoline<ParseResult> parseCatch(Parser parser, ParseResult tryBody, Trampoline.Continuation<ParseResult> continuation) {
        if (!parser.match(TokenType.CATCH)) {
            return parseFinally(parser, tryBody, null, -1, null, continuation);
        }
        // 可选的 catch 块
        String catchVarName;
        int position;
        if (parser.match(TokenType.LEFT_PAREN)) {
            catchVarName = parser.consume(TokenType.IDENTIFIER, "Expected variable name in catch clause").getLexeme();
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after catch variable");
            SymbolEnvironment env = parser.getSymbolEnvironment();
            env.defineVariable(catchVarName);
            position = env.getLocalVariable(catchVarName);
        } else {
            position = -1;
            catchVarName = null;
        }
        // 可选的 finally 块
        Trampoline.Continuation<ParseResult> catchContinuation = catchBody -> parseFinally(parser, tryBody, catchVarName, position, catchBody, continuation);
        if (parser.match(TokenType.LEFT_BRACE)) {
            return BlockParser.parse(parser, catchContinuation);
        }
        return ExpressionParser.parse(parser, catchContinuation);
    }

    private static Trampoline<ParseResult> parseFinally(Parser parser, ParseResult tryBody, String catchVarName, int position, ParseResult catchBody, Trampoline.Continuation<ParseResult> continuation) {
        if (!parser.match(TokenType.FINALLY)) {
            return Trampoline.more(() -> continuation.apply(new TryExpression(tryBody, catchVarName, position, catchBody, null)));
        }
        if (parser.match(TokenType.LEFT_BRACE)) {
            return BlockParser.parse(parser, finallyBody -> continuation.apply(new TryExpression(tryBody, catchVarName, position, catchBody, finallyBody)));
        }
        return ExpressionParser.parse(parser, finallyBody -> continuation.apply(new TryExpression(tryBody, catchVarName, position, catchBody, finallyBody)));
    }
}

