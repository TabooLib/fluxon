package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseException;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.expression.*;
import org.tabooproject.fluxon.parser.expression.literal.*;

/**
 * 表达式解析器 - 递归下降解析
 * <p>
 * 运算符优先级（从高到低）：
 * 1. Primary     - 字面量、标识符、括号、控制流、集合
 * 2. Call        - 函数调用()、上下文调用::
 * 3. Reference   - 引用&、可选引用&?
 * 4. Unary       - !、-、await
 * 5. Factor      - *、/、%
 * 6. Term        - +、-
 * 7. Comparison  - >、>=、<、<=
 * 8. Equality    - ==、!=
 * 9. Range       - ..、..<
 * 10. LogicalAnd - &&
 * 11. LogicalOr  - ||
 * 12. Ternary    - ? :
 * 13. Elvis      - ?:
 * 14. Assignment - =、+=、-=、*=、/=、%=
 * <p>
 */
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
            // 如果是引用表达式，解包成实际的目标
            if (expr instanceof ReferenceExpression) {
                expr = ((ReferenceExpression) expr).getIdentifier();
            }
            // 赋值操作符可以对 Identifier 或 IndexAccessExpression 使用
            if (expr instanceof Identifier) {
                String name = ((Identifier) expr).getValue();
                // 获取局部变量的位置
                // 先获取一次是因为可能是全局变量，此时 position 为 -1
                int position = parser.getSymbolEnvironment().getLocalVariable(name);
                // 只有 ASSIGN 才会将变量添加到当前作用域
                if (match == TokenType.ASSIGN && !parser.getSymbolEnvironment().hasVariable(name)) {
                    parser.defineVariable(name);
                    // 更新位置
                    position = parser.getSymbolEnvironment().getLocalVariable(name);
                }
                return new AssignExpression(expr, operator, parseAssignment(parser), position);
            } else if (expr instanceof IndexAccessExpression) {
                // 索引访问赋值 map["key"] = value
                return new AssignExpression(expr, operator, parseAssignment(parser), -1);
            }
            throw new ParseException("Invalid assignment target: " + expr, operator, parser.getResults());
        }
        return expr;
    }

    /**
     * 解析三元运算符表达式
     * 三元运算符 condition ? true_expr : false_expr
     *
     * @return 三元运算符表达式解析结果
     */
    public static ParseResult parseTernary(Parser parser) {
        ParseResult condition = parseLogicalOr(parser);
        if (parser.match(TokenType.QUESTION)) {
            ParseResult trueExpr = parseTernary(parser);
            parser.consume(TokenType.COLON, "Expected ':' after ternary true expression");
            ParseResult falseExpr = parseTernary(parser);
            return new TernaryExpression(condition, trueExpr, falseExpr);
        }
        return condition;
    }

    /**
     * 解析Elvis操作符表达式
     * Elvis操作符 ?: 用于提供默认值，例如：a ?: b 表示如果a为null则返回b，否则返回a
     *
     * @return Elvis操作符表达式解析结果
     */
    public static ParseResult parseElvis(Parser parser) {
        ParseResult expr = parseTernary(parser);
        if (parser.match(TokenType.QUESTION_COLON)) {
            expr = new ElvisExpression(expr, parseElvis(parser));
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
     * 解析一元表达式（负号、逻辑非）
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
            // 其他情况，解析引用表达式
            default:
                return parseReference(parser);
        }
    }

    /**
     * 解析引用表达式（&）
     * 引用的优先级高于一元操作符，低于调用操作
     *
     * @return 引用表达式解析结果
     */
    public static ParseResult parseReference(Parser parser) {
        if (parser.peek().getType() == TokenType.AMPERSAND) {
            parser.consume();                                      // 消费 &
            boolean isOptional = parser.match(TokenType.QUESTION); // 如果后面跟一个问号，则不进行合法性检查
            String name = parser.consume(TokenType.IDENTIFIER, "Expect variable name after '&'.").getLexeme();
            ParseResult ref;
            if (isOptional) {
                ref = new ReferenceExpression(new Identifier(name), true, -1);
            } else {
                // 检查函数或变量是否存在
                if (parser.getContext().isAllowInvalidReference() || parser.isFunction(name) || parser.hasVariable(name)) {
                    // 如果 isAllowInvalidReference 为真，默认启用 isOptional，避免运行时报 VariableNotFoundException
                    ref = new ReferenceExpression(new Identifier(name), parser.getContext().isAllowInvalidReference(), parser.getSymbolEnvironment().getLocalVariable(name));
                } else {
                    throw new VariableNotFoundException(name);
                }
            }
            // 引用后可能有后缀操作（[]、()）和上下文调用（::）
            // 先处理后缀操作
            ref = FunctionCallParser.parsePostfixOperations(parser, ref);
            // 再处理上下文调用
            return parseCallExpression(parser, ref);
        }
        // 不是引用，继续解析调用表达式
        return parseCallExpression(parser, FunctionCallParser.parse(parser));
    }

    /**
     * 解析调用表达式（处理已有表达式的后续调用操作）
     *
     * @param parser 解析器
     * @param expr   已解析的表达式
     * @return 应用了调用操作的表达式
     */
    private static ParseResult parseCallExpression(Parser parser, ParseResult expr) {
        // 处理上下文调用（::）
        while (parser.match(TokenType.CONTEXT_CALL)) {
            ParseResult context;
            // 如果右侧是代码块
            if (parser.match(TokenType.LEFT_BRACE)) {
                boolean isContextCall = parser.getSymbolEnvironment().isContextCall();
                parser.getSymbolEnvironment().setContextCall(true);
                context = BlockParser.parse(parser);
                parser.getSymbolEnvironment().setContextCall(isContextCall);
            }
            // 继续解析上下文调用的右侧
            else {
                SymbolEnvironment env = parser.getSymbolEnvironment();
                boolean isContextCall = env.isContextCall();
                env.setContextCall(true);
                // 右侧可能是引用或其他调用表达式
                context = parseReference(parser);
                env.setContextCall(isContextCall);
            }
            expr = new ContextCallExpression(expr, context);
        }
        return expr;
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
            case TRY:
                return TryParser.parse(parser);

            // 分组表达式
            case LEFT_PAREN: {
                parser.consume(); // 消费左括号
                ParseResult expr = parse(parser);
                parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
                return new GroupingExpression(expr);
            }
            // 代码块
            case LEFT_BRACE: {
                parser.consume(); // 消费左括号
                return BlockParser.parse(parser);
            }
            // 文件结束
            case EOF:
                throw new ParseException("Eof", parser.peek(), parser.getResults());
            default:
                throw new ParseException("Expected expression", parser.peek(), parser.getResults());
        }
    }
}

