package org.tabooproject.fluxon.parser.impl;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.expressions.WhenExpression;

import java.util.ArrayList;
import java.util.List;

public class WhenParser {

    /**
     * 解析 When 表达式
     *
     * @return When 表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        // 消费 WHEN 标记
        parser.consume(TokenType.WHEN, "Expected 'when' before when expression");

        // 则解析条件表达式
        ParseResult condition = null;
        if (!parser.check(TokenType.LEFT_BRACE)) {
            condition = ExpressionParser.parse(parser);
        }

        // 消费左花括号，如果存在的话
        parser.match(TokenType.LEFT_BRACE);

        // 如果当前是 EOF，直接返回空的 when 表达式
        if (parser.isAtEnd()) {
            return new WhenExpression(condition, new ArrayList<>());
        }

        // 解析分支
        List<WhenExpression.WhenBranch> branches = new ArrayList<>();

        while (!parser.check(TokenType.RIGHT_BRACE) && !parser.isAtEnd()) {
            // 解析非 else 分支条件
            ParseResult branchCondition = null;
            if (!parser.match(TokenType.ELSE)) {
                branchCondition = ExpressionParser.parse(parser);
            }
            // 消费箭头操作符
            parser.consume(TokenType.ARROW, "Expected '->' after else");
            // 解析分支结果
            branches.add(new WhenExpression.WhenBranch(branchCondition, ExpressionParser.parse(parser)));
            // 可选的分支结束符
            parser.match(TokenType.SEMICOLON);
        }

        // 消费右花括号，如果存在的话
        parser.match(TokenType.RIGHT_BRACE);
        return new WhenExpression(condition, branches);
    }
}
