package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseException;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.expression.*;
import org.tabooproject.fluxon.parser.expression.literal.*;

import java.util.Collections;

public class ExpressionParser {

    private static final int MAX_RECURSION_DEPTH = 1000;
    private static int currentDepth = 0;

    /**
     * 解析语句
     *
     * @return 解析结果
     */
    public static ParseResult parse(Parser parser) {
        if (currentDepth > MAX_RECURSION_DEPTH) {
            throw new ParseException("Maximum recursion depth exceeded", parser.peek(), parser.getResults());
        }
        currentDepth++;
        try {
            return parseAssignment(parser);
        } finally {
            currentDepth--;
        }
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
        TokenType match = parser.match(
                TokenType.ASSIGN,
                TokenType.PLUS_ASSIGN,
                TokenType.MINUS_ASSIGN,
                TokenType.MULTIPLY_ASSIGN,
                TokenType.DIVIDE_ASSIGN,
                TokenType.MODULO_ASSIGN
        );
        if (match != null) {
            Token operator = parser.previous();
            // 赋值操作符只能对 Identifier 使用
            if (expr instanceof Identifier) {
                String name = ((Identifier) expr).getValue();
                // 只有 ASSIGN 才会将变量添加到当前作用域
                if (match == TokenType.ASSIGN) {
                    parser.defineVariable(name);
                }
                return new Assignment(name, operator, parseAssignment(parser));
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
        ParseResult expr = parseContextCall(parser);
        if (parser.match(TokenType.QUESTION_COLON)) {
            expr = new ElvisExpression(expr, parseElvis(parser));
        }
        return expr;
    }

    /**
     * 解析上下文调用表达式
     * 上下文调用 :: 用于将左侧表达式作为上下文传递给右侧表达式
     * 例如：&list :: { print "Hello" } 表示将 &list 作为上下文传递给代码块
     *
     * @return 上下文调用表达式解析结果
     */
    public static ParseResult parseContextCall(Parser parser) {
        ParseResult expr = parseLogicalOr(parser);
        if (parser.match(TokenType.CONTEXT_CALL)) {
            ParseResult context;
            // 如果右侧是代码块，解析代码块
            if (parser.match(TokenType.LEFT_BRACE)) {
                context = BlockParser.parse(parser, Collections.emptyList(), false, false);
            } else {
                // 否则解析表达式
                context = parseContextCall(parser);
            }
            expr = new ContextCall(expr, context);
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
            expr = new LogicalExpression(expr, parser.previous(), parseLogicalAnd(parser));
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
            expr = new LogicalExpression(expr, parser.previous(), parseRange(parser));
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
        TokenType match = parser.match(TokenType.RANGE, TokenType.RANGE_EXCLUSIVE);
        if (match != null) {
            expr = new RangeExpression(expr, parseEquality(parser), match == TokenType.RANGE);
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
        while (parser.match(TokenType.EQUAL, TokenType.NOT_EQUAL) != null) {
            expr = new BinaryExpression(expr, parser.previous(), parseComparison(parser));
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
        while (parser.match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL) != null) {
            expr = new BinaryExpression(expr, parser.previous(), parseTerm(parser));
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
        while (parser.match(TokenType.PLUS, TokenType.MINUS) != null) {
            expr = new BinaryExpression(expr, parser.previous(), parseFactor(parser));
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
        while (parser.match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO) != null) {
            expr = new BinaryExpression(expr, parser.previous(), parseUnary(parser));
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
                return new UnaryExpression(parser.consume(), parseUnary(parser));
            // 等待
            case AWAIT: {
                parser.consume(); // 消费 await
                return new AwaitExpression(parseUnary(parser));
            }
            // 引用
            case AMPERSAND: {
                parser.consume(); // 消费 &
                String name = parser.consume(TokenType.IDENTIFIER, "Expect variable name after '&'.").getLexeme();
                // 检查函数或变量是否存在
                if (parser.isFunction(name) || parser.isVariable(name)) {
                    return new ReferenceExpression(new Identifier(name));
                } else {
                    throw new VariableNotFoundException(name + ", scope: " + parser.getCurrentScope().getAllVariables());
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
            // 标识符
            case IDENTIFIER:
                return new Identifier(parser.consume().getLexeme());

            // 字符串
            case STRING:
                return new StringLiteral(parser.consume().getLexeme());
            // 整型
            case INTEGER:
                return new IntLiteral((int) parser.consume().getValue());
            // 长整型
            case LONG:
                return new LongLiteral((long) parser.consume().getValue());
            // 单精度
            case FLOAT:
                return new FloatLiteral((float) parser.consume().getValue());
            // 双精度
            case DOUBLE:
                return new DoubleLiteral((double) parser.consume().getValue());
            // 真
            case TRUE: {
                parser.consume();
                return new BooleanLiteral(true);
            }
            // 假
            case FALSE: {
                parser.consume();
                return new BooleanLiteral(false);
            }
            // 空
            case NULL:
                parser.consume();
                return new NullLiteral();

            // 列表和字典
            case LEFT_BRACKET: {
                parser.consume(); // 消费左括号
                return ListParser.parse(parser);
            }

            // 表达式
            case IF:
                return IfParser.parse(parser);
            case FOR:
                return ForParser.parse(parser);
            case WHEN:
                return WhenParser.parse(parser);
            case WHILE:
                return WhileParser.parse(parser);

            // 分组表达式
            case LEFT_PAREN: {
                parser.consume(); // 消费左括号
                ParseResult expr = parse(parser);
                parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
                return new GroupingExpression(expr);
            }
            // 代码块
            case LEFT_BRACE: {
                return BlockParser.parse(parser, Collections.emptyList(), false, false);
            }
            // 文件结束
            case EOF:
                throw new ParseException("Eof", parser.peek(), parser.getResults());
            default:
                throw new ParseException("Expected expression", parser.peek(), parser.getResults());
        }
    }
}
