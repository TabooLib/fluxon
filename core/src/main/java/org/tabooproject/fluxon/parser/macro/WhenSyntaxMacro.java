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
        // 检查 else 分支
        if (parser.match(TokenType.ELSE)) {
            parser.consume(TokenType.ARROW, "Expected '->' after else");
            return ExpressionParser.parse(parser, branchResult -> {
                branches.add(new WhenExpression.WhenBranch(WhenExpression.MatchType.EQUAL, null, branchResult, null));
                parser.match(TokenType.SEMICOLON);
                return parseBranch(parser, condition, branches, continuation, whenToken);
            });
        }
        Token peek = parser.peek();
        WhenExpression.MatchType matchType;
        Class<?> targetClass;
        // 检查特殊匹配类型（必须在解析表达式之前）
        switch (peek.getType()) {
            case IS:
                parser.advance();
                matchType = WhenExpression.MatchType.IS;
                // 解析类型字面量并解析为 Class 对象
                targetClass = SyntaxMacroHelper.parseAndResolveType(parser);
                // 对于 IS 类型，直接跳到箭头和结果解析
                parser.consume(TokenType.ARROW, "Expected '->' after is type");
                Class<?> finalTargetClass = targetClass;
                return ExpressionParser.parse(parser, branchResult -> {
                    branches.add(new WhenExpression.WhenBranch(matchType, null, branchResult, finalTargetClass));
                    parser.match(TokenType.SEMICOLON);
                    return parseBranch(parser, condition, branches, continuation, whenToken);
                });
            case IN:
                parser.advance();
                matchType = WhenExpression.MatchType.CONTAINS;
                break;
            case NOT:
                parser.advance();
                if (parser.peek(1).getType() == TokenType.IN) {
                    parser.advance();
                    matchType = WhenExpression.MatchType.NOT_CONTAINS;
                } else {
                    throw new RuntimeException("Expected 'in' after 'not'");
                }
                break;
            default:
                matchType = WhenExpression.MatchType.EQUAL;
                break;
        }
        // 解析条件表达式和结果
        return ExpressionParser.parse(parser, branchCondition -> {
            parser.consume(TokenType.ARROW, "Expected '->' after condition");
            return ExpressionParser.parse(parser, branchResult -> {
                branches.add(new WhenExpression.WhenBranch(matchType, branchCondition, branchResult, null));
                parser.match(TokenType.SEMICOLON);
                return parseBranch(parser, condition, branches, continuation, whenToken);
            });
        });
    }
}