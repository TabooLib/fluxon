package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.statement.BreakStatement;
import org.tabooproject.fluxon.parser.statement.ContinueStatement;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;

import java.util.ArrayList;
import java.util.List;

public class StatementParser {

    public static ParseResult parseTopLevel(Parser parser) {
        // 检查注解
        List<Annotation> annotations = new ArrayList<>();
        while (parser.check(TokenType.AT)) {
            annotations.add(AnnotationParser.parse(parser));
        }
        // 检查顶层关键字
        TokenType match = parser.match(TokenType.SYNC, TokenType.ASYNC, TokenType.DEF, TokenType.RETURN);
        if (match != null) {
            Token matchedToken = parser.previous();
            switch (match) {
                // 同步函数定义
                case SYNC: {
                    parser.match(TokenType.DEF);
                    return FunctionDefinitionParser.parse(parser, false, true, annotations);
                }
                // 异步函数定义
                case ASYNC: {
                    parser.match(TokenType.DEF);
                    return FunctionDefinitionParser.parse(parser, true, false, annotations);
                }
                // 普通函数定义
                case DEF:
                    return FunctionDefinitionParser.parse(parser, false, false, annotations);
                // 返回值
                case RETURN: {
                    // 检查是否有返回值
                    if (parser.isEndOfExpression()) {
                        parser.match(TokenType.SEMICOLON);
                        return parser.attachSource(new ReturnStatement(null), matchedToken);
                    }
                    ParseResult returnValue = ExpressionParser.parse(parser);
                    parser.match(TokenType.SEMICOLON);
                    return parser.attachSource(new ReturnStatement(returnValue), matchedToken);
                }
                default:
            }
        }
        // 如果有注解但不是函数定义，报错
        if (!annotations.isEmpty()) {
            throw new RuntimeException("Annotations can only be applied to function definitions");
        }
        // 解析表达式语句
        ParseResult expr = ExpressionParser.parse(parser);
        parser.match(TokenType.SEMICOLON);
        return parser.copySource(new ExpressionStatement(expr), expr);
    }

    /**
     * CPS 形式的子语句解析，方便与 Block/表达式 trampoline 串联。
     */
    public static Trampoline<ParseResult> parseSub(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        // 检查特殊语法
        TokenType match = parser.match(TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE);
        if (match != null) {
            Token matchedToken = parser.previous();
            switch (match) {
                // 返回值
                case RETURN: {
                    // 检查是否有返回值
                    if (parser.isEndOfExpression()) {
                        parser.match(TokenType.SEMICOLON);
                        return continuation.apply(parser.attachSource(new ReturnStatement(null), matchedToken));
                    }
                    return ExpressionParser.parse(parser, returnValue -> {
                        parser.match(TokenType.SEMICOLON);
                        return continuation.apply(parser.attachSource(new ReturnStatement(returnValue), matchedToken));
                    });
                }
                // 跳出循环
                case BREAK: {
                    // 是否允许跳出循环
                    if (parser.getSymbolEnvironment().isBreakable()) {
                        parser.match(TokenType.SEMICOLON);
                        return continuation.apply(parser.attachSource(new BreakStatement(), matchedToken));
                    }
                    throw new RuntimeException("Break outside of loop");
                }
                // 继续循环
                case CONTINUE: {
                    // 是否允许继续循环
                    if (parser.getSymbolEnvironment().isContinuable()) {
                        parser.match(TokenType.SEMICOLON);
                        return continuation.apply(parser.attachSource(new ContinueStatement(), matchedToken));
                    }
                    throw new RuntimeException("Continue outside of loop");
                }
                default:
            }
        }
        // 解析表达式语句
        return ExpressionParser.parse(parser, expr -> {
            parser.match(TokenType.SEMICOLON);
            return continuation.apply(parser.copySource(new ExpressionStatement(expr), expr));
        });
    }
}
