package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.IfExpression;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.type.BlockParser;
import org.tabooproject.fluxon.parser.type.ExpressionParser;
import org.tabooproject.fluxon.parser.type.StatementParser;

/**
 * If 表达式语法宏
 */
public class IfSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.IF);
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token ifToken = parser.consume(TokenType.IF, "Expected 'if' before if expression");
        return StatementParser.parseSubToExpr(parser, normalized ->
                parseThenBranch(parser, normalized, continuation, ifToken)
        );
    }

    private Trampoline<ParseResult> parseThenBranch(Parser parser, ParseResult condition, Trampoline.Continuation<ParseResult> continuation, Token ifToken) {
        parser.match(TokenType.THEN);
        return StatementParser.parseSubToExpr(parser, normalized ->
                parseElseBranch(parser, condition, normalized, continuation, ifToken)
        );
    }

    private Trampoline<ParseResult> parseElseBranch(Parser parser, ParseResult condition, ParseResult thenBranch, Trampoline.Continuation<ParseResult> continuation, Token ifToken) {
        if (parser.match(TokenType.ELSE)) {
            return StatementParser.parseSubToExpr(parser, normalized ->
                    continuation.apply(parser.attachSource(new IfExpression(condition, thenBranch, normalized), ifToken))
            );
        }
        return Trampoline.more(() -> continuation.apply(parser.attachSource(new IfExpression(condition, thenBranch, null), ifToken)));
    }
}
