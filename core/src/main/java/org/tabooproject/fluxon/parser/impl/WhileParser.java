package org.tabooproject.fluxon.parser.impl;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.expressions.WhileExpression;

import java.util.Collections;

public class WhileParser {

    /**
     * 解析 While 表达式
     *
     * @return While 表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        // 消费 WHILE 标记
        parser.consume(TokenType.WHILE, "Expected 'while' before while expression");
        // 解析条件表达式
        ParseResult condition = ExpressionParser.parse(parser);
        // 解析循环体
        ParseResult body;
        // 如果有左大括号，则解析为 Block 函数体
        if (parser.match(TokenType.LEFT_BRACE)) {
            body = BlockParser.parse(parser, Collections.emptyList());
        } else {
            body = ExpressionParser.parse(parser);
        }
        return new WhileExpression(condition, body);
    }
}
