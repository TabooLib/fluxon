package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
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
            switch (match) {
                // 同步函数定义
                case SYNC: {
                    parser.match(TokenType.DEF); // 消费 DEF
                    return FunctionDefinitionParser.parse(parser, false, true, annotations);
                }
                // 异步函数定义
                case ASYNC: {
                    parser.match(TokenType.DEF); // 消费 DEF
                    return FunctionDefinitionParser.parse(parser, true, false, annotations);
                }
                // 普通函数定义
                case DEF:
                    return FunctionDefinitionParser.parse(parser, false, false, annotations);
                // 返回值
                case RETURN: {
                    // 检查是否有返回值
                    if (parser.isEndOfExpression()) {
                        parser.match(TokenType.SEMICOLON); // 可选的分号
                        return new ReturnStatement(null);
                    }
                    ParseResult returnValue = ExpressionParser.parse(parser);
                    parser.match(TokenType.SEMICOLON); // 可选的分号
                    return new ReturnStatement(returnValue);
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
        parser.match(TokenType.SEMICOLON); // 可选的分号
        return new ExpressionStatement(expr);
    }

    public static ParseResult parseSub(Parser parser) {
        // 检查特殊语法
        TokenType match = parser.match(TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE);
        if (match != null) {
            switch (match) {
                // 返回值
                case RETURN: {
                    // 检查是否有返回值
                    if (parser.isEndOfExpression()) {
                        parser.match(TokenType.SEMICOLON); // 可选的分号
                        return new ReturnStatement(null);
                    }
                    ParseResult returnValue = ExpressionParser.parse(parser);
                    parser.match(TokenType.SEMICOLON); // 可选的分号
                    return new ReturnStatement(returnValue);
                }
                // 跳出循环
                case BREAK: {
                    // 是否允许跳出循环
                    if (parser.getSymbolEnvironment().isBreakable()) {
                        parser.match(TokenType.SEMICOLON); // 可选的分号
                        return new BreakStatement();
                    }
                    throw new RuntimeException("Break outside of loop");
                }
                // 继续循环
                case CONTINUE: {
                    // 是否允许继续循环
                    if (parser.getSymbolEnvironment().isContinuable()) {
                        parser.match(TokenType.SEMICOLON); // 可选的分号
                        return new ContinueStatement();
                    }
                    throw new RuntimeException("Continue outside of loop");
                }
                default:
            }
        }
        // 解析表达式语句
        ParseResult expr = ExpressionParser.parse(parser);
        parser.match(TokenType.SEMICOLON); // 可选的分号
        return new ExpressionStatement(expr);
    }
}
