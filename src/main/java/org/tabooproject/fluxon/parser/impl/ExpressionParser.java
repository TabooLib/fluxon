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
        switch (parser.peek().getType()) {
            // 逻辑非、负号
            case NOT:
            case MINUS:
                return new Expressions.UnaryExpression(parser.consume(), parsePrimary(parser));
            // 等待
            case AWAIT: {
                parser.consume(); // 消费 await
                return new Expressions.AwaitExpression(parseUnary(parser));
            }
            // 引用
            case AMPERSAND: {
                parser.consume(); // 消费 &
                String name = parser.consume(TokenType.IDENTIFIER, "Expect variable name after '&'.").getValue();
                // 检查变量是否存在
                if (!parser.isVariable(name)) {
                    throw new RuntimeException("Unknown variable '" + name + "'.");
                } else {
                    return new Expressions.ReferenceExpression(new Expressions.Variable(name));
                }
            }
            // 函数调用
            default:
                return FunctionCallParser.parse(parser);
        }
    }

    /**
     * 解析基本表达式
     *
     * @return 基本表达式解析结果
     */
    public static ParseResult parsePrimary(Parser parser) {
        switch (parser.peek().getType()) {
            // 真
            case TRUE: {
                parser.consume();
                return new Expressions.BooleanLiteral(true);
            }
            // 假
            case FALSE: {
                parser.consume();
                return new Expressions.BooleanLiteral(false);
            }
            // 整型
            case INTEGER:
                return new Expressions.IntegerLiteral(parser.consume().getValue());
            // 浮点数
            case FLOAT:
                return new Expressions.FloatLiteral(parser.consume().getValue());
            // 字符串
            case STRING:
                return new Expressions.StringLiteral(parser.consume().getValue());
            // 变量引用
            case IDENTIFIER:
                return new Expressions.Variable(parser.consume().getValue());
            // 表达式
            case IF:
                return IfParser.parse(parser);
            case WHEN:
                return WhenParser.parse(parser);
            case WHILE:
                return WhileParser.parse(parser);
            // 列表和字典
            case LEFT_BRACKET: {
                parser.consume(); // 消费左括号
                return ListParser.parse(parser);
            }
            // 分组表达式
            case LEFT_PAREN: {
                parser.consume(); // 消费左括号
                ParseResult expr = parse(parser);
                parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
                return new Expressions.GroupingExpression(expr);
            }
            // 文件结束
            case EOF:
                throw new ParseException("Eof", parser.peek(), parser.getResults());
                // 未知符号
            default:
                throw new ParseException("Expected expression", parser.peek(), parser.getResults());
        }
    }
}
