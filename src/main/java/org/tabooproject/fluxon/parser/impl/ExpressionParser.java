package org.tabooproject.fluxon.parser.impl;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseException;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.expressions.Expressions;
import org.tabooproject.fluxon.parser.statements.Statements;

import java.util.ArrayList;

public class ExpressionParser {

    /**
     * 解析语句
     *
     * @return 解析结果
     */
    public static ParseResult parse(Parser parser) {
        return parseAssignment(parser);
    }

    /**
     * 解析赋值表达式
     * <p>
     * 为什么要先解析逻辑或表达式，然后再解析赋值表达式？
     * 在解析器设计中，通常会使用递归下降解析（Recursive Descent Parsing）的方法来处理不同优先级的表达式。
     * 即当解析某一层次的表达式时，它会先调用解析更高优先级表达式的方法，然后再处理当前层次的操作符。
     * 这种设计确保了表达式 a = b || c 被正确解析为 a = (b || c) 而不是 (a = b) || c。
     *
     * @return 赋值表达式解析结果
     */
    public static ParseResult parseAssignment(Parser parser) {
        ParseResult expr = parseElvis(parser);

        if (parser.match(TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN, TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN)) {
            Token operator = parser.previous();
            ParseResult value = parseAssignment(parser);

            // 检查左侧是否为有效的赋值目标
            if (expr instanceof Expressions.Variable) {
                String name = ((Expressions.Variable) expr).getName();
                
                // 将变量添加到当前作用域
                parser.defineVariable(name);
                
                return new Expressions.Assignment(name, operator, value);
            }
            throw new ParseException("Invalid assignment target", operator, parser.getResults());
        }
        return expr;
    }

    /**
     * 解析Elvis操作符表达式
     * Elvis操作符 ?: 用于提供默认值，例如：a ?: b 表示如果a为null则返回b，否则返回a
     *
     * @return Elvis操作符表达式解析结果
     */
    public static ParseResult parseElvis(Parser parser) {
        ParseResult expr = parseLogicalOr(parser);

        // 检查是否有Elvis操作符
        if (parser.match(TokenType.QUESTION_COLON)) {
            // 解析默认值表达式
            ParseResult alternative = parseElvis(parser);
            expr = new Expressions.ElvisExpression(expr, alternative);
        }
        return expr;
    }

    /**
     * 解析逻辑或表达式
     *
     * @return 逻辑或表达式解析结果
     */
    public static ParseResult parseLogicalOr(Parser parser) {
        ParseResult expr = parseLogicalAnd(parser);

        while (parser.match(TokenType.OR)) {
            Token operator = parser.previous();
            ParseResult right = parseLogicalAnd(parser);
            expr = new Expressions.LogicalExpression(expr, operator, right);
        }
        return expr;
    }

    /**
     * 解析逻辑与表达式
     *
     * @return 逻辑与表达式解析结果
     */
    public static ParseResult parseLogicalAnd(Parser parser) {
        ParseResult expr = parseRange(parser);

        while (parser.match(TokenType.AND)) {
            Token operator = parser.previous();
            ParseResult right = parseEquality(parser);
            expr = new Expressions.LogicalExpression(expr, operator, right);
        }
        return expr;
    }

    /**
     * 解析范围表达式
     * 范围表达式用于创建一个范围，例如：1..10 表示从1到10的范围（包含10）
     * 1..<10 表示从1到9的范围（不包含10）
     *
     * @return 范围表达式解析结果
     */
    public static ParseResult parseRange(Parser parser) {
        ParseResult expr = parseEquality(parser);

        // 检查是否有范围操作符
        if (parser.match(TokenType.RANGE, TokenType.RANGE_EXCLUSIVE)) {
            Token operator = parser.previous();
            boolean inclusive = operator.getType() == TokenType.RANGE;

            // 解析范围的结束表达式
            ParseResult end = parseEquality(parser);
            expr = new Expressions.RangeExpression(expr, end, inclusive);
        }
        return expr;
    }

    /**
     * 解析相等性表达式（等于、不等于）
     *
     * @return 相等性表达式解析结果
     */
    public static ParseResult parseEquality(Parser parser) {
        ParseResult expr = parseComparison(parser);

        while (parser.match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            Token operator = parser.previous();
            ParseResult right = parseComparison(parser);
            expr = new Expressions.BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    /**
     * 解析比较表达式（大于、大于等于、小于、小于等于）
     *
     * @return 比较表达式解析结果
     */
    public static ParseResult parseComparison(Parser parser) {
        ParseResult expr = parseTerm(parser);

        while (parser.match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = parser.previous();
            ParseResult right = parseTerm(parser);
            expr = new Expressions.BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    /**
     * 解析项表达式（加、减）
     *
     * @return 项表达式解析结果
     */
    public static ParseResult parseTerm(Parser parser) {
        ParseResult expr = parseFactor(parser);

        while (parser.match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = parser.previous();
            ParseResult right = parseFactor(parser);
            expr = new Expressions.BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    /**
     * 解析因子表达式（乘、除、取模）
     *
     * @return 因子表达式解析结果
     */
    public static ParseResult parseFactor(Parser parser) {
        ParseResult expr = parseUnary(parser);

        while (parser.match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) {
            Token operator = parser.previous();
            ParseResult right = parseUnary(parser);
            expr = new Expressions.BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    /**
     * 解析一元表达式（负号、逻辑非、引用）
     *
     * @return 一元表达式解析结果
     */
    public static ParseResult parseUnary(Parser parser) {
        if (parser.match(TokenType.NOT, TokenType.MINUS)) {
            Token operator = parser.previous();
            ParseResult right = parsePrimary(parser);
            return new Expressions.UnaryExpression(operator, right);
        }

        if (parser.match(TokenType.AWAIT)) {
            Token operator = parser.previous();
            ParseResult right = parseUnary(parser);
            return new Expressions.AwaitExpression(right);
        }

        if (parser.match(TokenType.AMPERSAND)) {
            Token operator = parser.previous();
            ParseResult right = parsePrimary(parser);
            return new Expressions.ReferenceExpression(right);
        }
        return FunctionCallParser.parse(parser);
    }

    /**
     * 解析基本表达式
     *
     * @return 基本表达式解析结果
     */
    public static ParseResult parsePrimary(Parser parser) {
        // 字面量
        if (parser.match(TokenType.FALSE)) {
            return new Expressions.BooleanLiteral(false);
        }
        if (parser.match(TokenType.TRUE)) {
            return new Expressions.BooleanLiteral(true);
        }
        if (parser.match(TokenType.INTEGER)) {
            return new Expressions.IntegerLiteral(parser.previous().getValue());
        }
        if (parser.match(TokenType.FLOAT)) {
            return new Expressions.FloatLiteral(parser.previous().getValue());
        }
        if (parser.match(TokenType.STRING)) {
            return new Expressions.StringLiteral(parser.previous().getValue());
        }
        if (parser.match(TokenType.LEFT_BRACKET)) {
            return ListParser.parse(parser);
        }

        // 变量引用
        if (parser.match(TokenType.IDENTIFIER)) {
            String name = parser.previous().getValue();
            return new Expressions.Variable(name);
            
            // 注意：这里不需要将变量添加到符号表
            // 因为变量引用不一定是变量声明
            // 只有在赋值表达式中，才会将变量添加到符号表
            // 例如：a = 1 会将a添加到符号表
            // 但是：print(a) 中的a只是引用，不会添加到符号表
            // 如果a不存在，会在运行时报错
        }

        // 分组表达式
        if (parser.match(TokenType.LEFT_PAREN)) {
            ParseResult expr = parse(parser);
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return new Expressions.GroupingExpression(expr);
        }

        // When 表达式
        if (parser.check(TokenType.WHEN)) {
            return WhenParser.parse(parser);
        }

        // If 表达式
        if (parser.check(TokenType.IF)) {
            return IfParser.parse(parser);
        }

        // While 表达式
        if (parser.check(TokenType.WHILE)) {
            return WhileParser.parse(parser);
        }

        // Eof
        Token peek = parser.peek();
        if (peek.getType() == TokenType.EOF) {
            throw new ParseException("Eof", peek, parser.getResults());
        } else {
            throw new ParseException("Expected expression", peek, parser.getResults());
        }
    }
}
