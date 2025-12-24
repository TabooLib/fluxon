package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.WhenExpression;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

import java.util.ArrayList;
import java.util.List;

/**
 * When 表达式语法宏
 */
public class WhenSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.WHEN);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token whenToken = parser.consume(TokenType.WHEN, "Expected 'when' before when expression");
        return parseCondition(parser, condition -> parseBranches(parser, condition, continuation, whenToken));
    }

    private Trampoline<ParseResult> parseCondition(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        if (!parser.check(TokenType.LEFT_BRACE)) {
            return ExpressionParser.parse(parser, expr -> {
                parser.match(TokenType.LEFT_BRACE);
                return continuation.apply(expr);
            });
        }
        parser.match(TokenType.LEFT_BRACE);
        return continuation.apply(null);
    }

    private Trampoline<ParseResult> parseBranches(Parser parser, ParseResult condition, Trampoline.Continuation<ParseResult> continuation, Token whenToken) {
        if (parser.isAtEnd()) {
            return Trampoline.more(() -> continuation.apply(parser.attachSource(new WhenExpression(condition, new ArrayList<>()), whenToken)));
        }
        List<WhenExpression.WhenBranch> branches = new ArrayList<>();
        return Trampoline.more(() -> parseBranch(parser, condition, branches, continuation, whenToken));
    }

    private Trampoline<ParseResult> parseBranch(Parser parser, ParseResult condition, List<WhenExpression.WhenBranch> branches, Trampoline.Continuation<ParseResult> continuation, Token whenToken) {
        if (parser.check(TokenType.RIGHT_BRACE) || parser.isAtEnd()) {
            parser.match(TokenType.RIGHT_BRACE);
            return continuation.apply(parser.attachSource(new WhenExpression(condition, branches), whenToken));
        }

        Token peek = parser.peek();
        WhenExpression.MatchType matchType;
        if (peek.getType() == TokenType.IN) {
            parser.advance();
            matchType = WhenExpression.MatchType.CONTAINS;
        } else if (peek.getType() == TokenType.NOT) {
            parser.advance();
            if (parser.peek(1).getType() == TokenType.IN) {
                parser.advance();
                matchType = WhenExpression.MatchType.NOT_CONTAINS;
            } else {
                throw new RuntimeException("Expected 'in' after 'not'");
            }
        } else {
            matchType = WhenExpression.MatchType.EQUAL;
        }

        // 解析分支条件与结果，递归构建分支列表
        Trampoline.Continuation<ParseResult> addBranch = branchCondition -> {
            parser.consume(TokenType.ARROW, "Expected '->' after else");
            return ExpressionParser.parse(parser, branchResult -> {
                branches.add(new WhenExpression.WhenBranch(matchType, branchCondition, branchResult));
                parser.match(TokenType.SEMICOLON);
                return parseBranch(parser, condition, branches, continuation, whenToken);
            });
        };

        if (parser.match(TokenType.ELSE)) {
            return addBranch.apply(null);
        }
        return ExpressionParser.parse(parser, addBranch);
    }
}
