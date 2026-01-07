package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.TryExpression;
import org.tabooproject.fluxon.parser.type.BlockParser;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

/**
 * Try 表达式语法宏
 * <p>
 * 语法：try { ... } [catch (var) { ... }] [finally { ... }]
 */
public class TrySyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.TRY);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token tryToken = parser.consume(TokenType.TRY, "Expected 'try' before try expression");
        return parseTryBody(parser, tryBody -> parseCatch(parser, tryBody, continuation, tryToken));
    }

    private Trampoline<ParseResult> parseTryBody(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        if (parser.match(TokenType.LEFT_BRACE)) {
            return BlockParser.parse(parser, continuation);
        }
        return ExpressionParser.parse(parser, continuation);
    }

    private Trampoline<ParseResult> parseCatch(Parser parser, ParseResult tryBody, Trampoline.Continuation<ParseResult> continuation, Token tryToken) {
        if (!parser.match(TokenType.CATCH)) {
            return parseFinally(parser, tryBody, null, -1, null, continuation, tryToken);
        }
        String catchVarName;
        int position;
        if (parser.match(TokenType.LEFT_PAREN)) {
            catchVarName = parser.consume(TokenType.IDENTIFIER, "Expected variable name in catch clause").getLexeme();
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after catch variable");
            SymbolEnvironment env = parser.getSymbolEnvironment();
            env.defineLocalVariable(catchVarName);
            position = env.getLocalVariable(catchVarName);
        } else {
            position = -1;
            catchVarName = null;
        }
        // 可选的 finally 块
        Trampoline.Continuation<ParseResult> catchContinuation = catchBody -> parseFinally(parser, tryBody, catchVarName, position, catchBody, continuation, tryToken);
        if (parser.match(TokenType.LEFT_BRACE)) {
            return BlockParser.parse(parser, catchContinuation);
        }
        return ExpressionParser.parse(parser, catchContinuation);
    }

    private Trampoline<ParseResult> parseFinally(Parser parser, ParseResult tryBody, String catchVarName, int position, ParseResult catchBody, Trampoline.Continuation<ParseResult> continuation, Token tryToken) {
        if (!parser.match(TokenType.FINALLY)) {
            return Trampoline.more(() -> continuation.apply(parser.attachSource(new TryExpression(tryBody, catchVarName, position, catchBody, null), tryToken)));
        }
        if (parser.match(TokenType.LEFT_BRACE)) {
            return BlockParser.parse(parser, finallyBody -> continuation.apply(parser.attachSource(new TryExpression(tryBody, catchVarName, position, catchBody, finallyBody), tryToken)));
        }
        return ExpressionParser.parse(parser, finallyBody -> continuation.apply(parser.attachSource(new TryExpression(tryBody, catchVarName, position, catchBody, finallyBody), tryToken)));
    }
}
