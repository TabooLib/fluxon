package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.parselet.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优先级爬升解析器
 * 用于处理表达式的优先级和结合性
 */
public class PrattParser {

    public final Parser parser;

    // 前缀解析函数表
    public final Map<TokenType, PrefixParselet> prefixParselets = new HashMap<>();

    // 中缀解析函数表
    public final Map<TokenType, InfixParselet> infixParselets = new HashMap<>();

    /**
     * 创建优先级爬升解析器
     *
     * @param parser 语法分析器
     */
    public PrattParser(Parser parser) {
        this.parser = parser;

        // 注册前缀解析函数
        registerPrefixParselet(TokenType.IDENTIFIER, this::parseIdentifier);
        registerPrefixParselet(TokenType.STRING, this::parseLiteral);
        registerPrefixParselet(TokenType.INTEGER, this::parseLiteral);
        registerPrefixParselet(TokenType.FLOAT, this::parseLiteral);
        registerPrefixParselet(TokenType.BOOLEAN, this::parseLiteral);
        registerPrefixParselet(TokenType.LEFT_PAREN, this::parseGrouping);
        registerPrefixParselet(TokenType.AMPERSAND, this::parseVariable);
        registerPrefixParselet(TokenType.MINUS, this::parseUnary);
        registerPrefixParselet(TokenType.NOT, this::parseUnary);
        registerPrefixParselet(TokenType.LEFT_BRACKET, this::parseList);
        registerPrefixParselet(TokenType.LEFT_BRACE, this::parseMap);

        // 注册中缀解析函数
        registerInfixParselet(TokenType.PLUS, new BinaryOperatorParselet(this), Precedence.TERM);
        registerInfixParselet(TokenType.MINUS, new BinaryOperatorParselet(this), Precedence.TERM);
        registerInfixParselet(TokenType.MULTIPLY, new BinaryOperatorParselet(this), Precedence.FACTOR);
        registerInfixParselet(TokenType.DIVIDE, new BinaryOperatorParselet(this), Precedence.FACTOR);
        registerInfixParselet(TokenType.MODULO, new BinaryOperatorParselet(this), Precedence.FACTOR);
        registerInfixParselet(TokenType.EQUAL, new BinaryOperatorParselet(this), Precedence.EQUALITY);
        registerInfixParselet(TokenType.NOT_EQUAL, new BinaryOperatorParselet(this), Precedence.EQUALITY);
        registerInfixParselet(TokenType.GREATER, new BinaryOperatorParselet(this), Precedence.COMPARISON);
        registerInfixParselet(TokenType.LESS, new BinaryOperatorParselet(this), Precedence.COMPARISON);
        registerInfixParselet(TokenType.GREATER_EQUAL, new BinaryOperatorParselet(this), Precedence.COMPARISON);
        registerInfixParselet(TokenType.LESS_EQUAL, new BinaryOperatorParselet(this), Precedence.COMPARISON);
        registerInfixParselet(TokenType.AND, new BinaryOperatorParselet(this), Precedence.LOGICAL_AND);
        registerInfixParselet(TokenType.OR, new BinaryOperatorParselet(this), Precedence.LOGICAL_OR);
        registerInfixParselet(TokenType.QUESTION_DOT, new SafeAccessParselet(this), Precedence.CALL);
        registerInfixParselet(TokenType.QUESTION_COLON, new ElvisParselet(this), Precedence.CONDITIONAL);
        registerInfixParselet(TokenType.RANGE, new RangeParselet(this), Precedence.RANGE);
        registerInfixParselet(TokenType.RANGE_EXCLUSIVE, new RangeParselet(this), Precedence.RANGE);

        // 注册函数调用解析器
        registerInfixParselet(TokenType.IDENTIFIER, new CallParselet(this), Precedence.CALL);

        // 注册括号函数调用解析器，用于处理带括号的函数调用：func(arg1, arg2)
        registerInfixParselet(TokenType.LEFT_PAREN, new CallParselet(this), Precedence.CALL);
    }

    /**
     * 解析表达式
     *
     * @param precedence 当前优先级
     * @return 表达式节点
     */
    public Expr parseExpression(int precedence) {
        Token token = parser.peek();
        parser.advance();

        PrefixParselet prefix = prefixParselets.get(token.getType());
        if (prefix == null) {
            throw new Parser.ParseError("Expected expression, got " + token.getType() + " " + token.getValue());
        }

        Expr left = prefix.parse(token);

        while (precedence < getPrecedence()) {
            token = parser.peek();
            parser.advance();

            InfixParselet infix = infixParselets.get(token.getType());
            if (infix == null) {
                parser.current--; // 回退一个词法单元，以便下一次解析
                break;
            }
            left = infix.parse(left, token);
        }
        return left;
    }

    /**
     * 获取当前词法单元的优先级
     *
     * @return 优先级
     */
    public int getPrecedence() {
        InfixParselet parselet = infixParselets.get(parser.peek().getType());
        if (parselet != null) {
            return parselet.getPrecedence();
        }
        return 0;
    }

    /**
     * 注册前缀解析函数
     *
     * @param type     词法单元类型
     * @param parselet 解析函数
     */
    public void registerPrefixParselet(TokenType type, PrefixParselet parselet) {
        prefixParselets.put(type, parselet);
    }

    /**
     * 注册中缀解析函数
     *
     * @param type       词法单元类型
     * @param parselet   解析函数
     * @param precedence 优先级
     */
    public void registerInfixParselet(TokenType type, InfixParselet parselet, int precedence) {
        parselet.setPrecedence(precedence);
        infixParselets.put(type, parselet);
    }

    /**
     * 解析标识符
     *
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseIdentifier(Token token) {
        // 在 Fluxon 中，未加引号的标识符在参数位置自动转为字符串
        return new LiteralExpr(token.getValue(), LiteralExpr.LiteralType.STRING,
                new SourceLocation(token.getLine(), token.getColumn(), token.getLine(), token.getColumn() + token.getValue().length()));
    }

    /**
     * 解析字面量
     *
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseLiteral(Token token) {
        Object value;
        LiteralExpr.LiteralType type;
        switch (token.getType()) {
            case STRING:
                value = token.getValue();
                type = LiteralExpr.LiteralType.STRING;
                break;
            case INTEGER:
                value = Integer.parseInt(token.getValue());
                type = LiteralExpr.LiteralType.INTEGER;
                break;
            case FLOAT:
                value = Double.parseDouble(token.getValue());
                type = LiteralExpr.LiteralType.FLOAT;
                break;
            case BOOLEAN:
                value = Boolean.parseBoolean(token.getValue());
                type = LiteralExpr.LiteralType.BOOLEAN;
                break;
            default:
                throw new Parser.ParseError("Unexpected literal type: " + token.getType());
        }

        return new LiteralExpr(value, type,
                new SourceLocation(token.getLine(), token.getColumn(), token.getLine(), token.getColumn() + token.getValue().length()));
    }

    /**
     * 解析分组表达式
     *
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseGrouping(Token token) {
        Expr expr = parseExpression(0);
        parser.consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
        return expr;
    }

    /**
     * 解析变量引用
     *
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseVariable(Token token) {
        // 消费变量名
        Token name = parser.consume(TokenType.IDENTIFIER, "Expect variable name after '&'.");
        return new VariableExpr(name.getValue(),
                new SourceLocation(token.getLine(), token.getColumn(), name.getLine(), name.getColumn() + name.getValue().length()));
    }

    /**
     * 解析一元表达式
     *
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseUnary(Token token) {
        // 解析操作数
        Expr operand = parseExpression(Precedence.UNARY);

        UnaryExpr.Operator operator;
        switch (token.getType()) {
            case MINUS:
                operator = UnaryExpr.Operator.NEGATE;
                break;
            case NOT:
                operator = UnaryExpr.Operator.NOT;
                break;
            default:
                throw new Parser.ParseError("Unexpected unary operator: " + token.getType());
        }

        return new UnaryExpr(operand, operator,
                new SourceLocation(token.getLine(), token.getColumn(), operand.getLocation().getEndLine(), operand.getLocation().getEndColumn()));
    }

    /**
     * 解析二元表达式
     *
     * @param left  左操作数
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseBinary(Expr left, Token token) {
        // 解析右操作数
        Expr right = parseExpression(getPrecedence());

        BinaryExpr.Operator operator;
        switch (token.getType()) {
            case PLUS:
                operator = BinaryExpr.Operator.ADD;
                break;
            case MINUS:
                operator = BinaryExpr.Operator.SUBTRACT;
                break;
            case MULTIPLY:
                operator = BinaryExpr.Operator.MULTIPLY;
                break;
            case DIVIDE:
                operator = BinaryExpr.Operator.DIVIDE;
                break;
            case MODULO:
                operator = BinaryExpr.Operator.MODULO;
                break;
            case EQUAL:
                operator = BinaryExpr.Operator.EQUAL;
                break;
            case NOT_EQUAL:
                operator = BinaryExpr.Operator.NOT_EQUAL;
                break;
            case GREATER:
                operator = BinaryExpr.Operator.GREATER;
                break;
            case LESS:
                operator = BinaryExpr.Operator.LESS;
                break;
            case GREATER_EQUAL:
                operator = BinaryExpr.Operator.GREATER_EQUAL;
                break;
            case LESS_EQUAL:
                operator = BinaryExpr.Operator.LESS_EQUAL;
                break;
            case AND:
                operator = BinaryExpr.Operator.AND;
                break;
            case OR:
                operator = BinaryExpr.Operator.OR;
                break;
            default:
                throw new Parser.ParseError("Unexpected binary operator: " + token.getType());
        }

        return new BinaryExpr(left, right, operator,
                new SourceLocation(left.getLocation().getStartLine(), left.getLocation().getStartColumn(),
                        right.getLocation().getEndLine(), right.getLocation().getEndColumn()));
    }

    /**
     * 解析列表表达式
     *
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseList(Token token) {
        List<Expr> elements = new ArrayList<>();

        // 解析列表元素
        if (!parser.check(TokenType.RIGHT_BRACKET)) {
            do {
                elements.add(parseExpression(0));
            } while (parser.match(TokenType.COMMA));
        }

        // 消费右括号
        Token end = parser.consume(TokenType.RIGHT_BRACKET, "Expect ']' after list elements.");

        // 创建列表表达式
        return new ListExpr(elements,
                new SourceLocation(token.getLine(), token.getColumn(),
                        end.getLine(),
                        end.getColumn()));

    }

    /**
     * 解析字典表达式
     *
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseMap(Token token) {
        List<MapExpr.Entry> entries = new ArrayList<>();

        // 解析字典键值对
        if (!parser.check(TokenType.RIGHT_BRACE)) {
            do {
                // 解析键
                Expr key = parseExpression(0);

                // 解析冒号
                parser.consume(TokenType.COLON, "Expect ':' after map key.");

                // 解析值
                Expr value = parseExpression(0);

                entries.add(new MapExpr.Entry(key, value));
            } while (parser.match(TokenType.COMMA));
        }

        // 消费右大括号
        Token end = parser.consume(TokenType.RIGHT_BRACE, "Expect '}' after map entries.");

        // 创建字典表达式
        return new MapExpr(entries,
                new SourceLocation(token.getLine(), token.getColumn(),
                        end.getLine(), end.getColumn()));
    }

    /**
     * 解析空安全访问表达式
     *
     * @param left  左操作数
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseSafeAccess(Expr left, Token token) {
        // 消费属性名
        Token property = parser.consume(TokenType.IDENTIFIER, "Expect property name after '?.'.");

        // 创建空安全访问表达式
        Expr safeAccess = new SafeAccessExpr(left, property.getValue(),
                new SourceLocation(left.getLocation().getStartLine(), left.getLocation().getStartColumn(),
                        property.getLine(), property.getColumn() + property.getValue().length()));

        // 如果下一个词法单元是 '?.', 则继续解析链式调用
        // 这里简化处理，不支持链式调用
        return safeAccess;
    }

    /**
     * 解析 Elvis 操作符表达式
     *
     * @param left  左操作数
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseElvis(Expr left, Token token) {
        // 解析备选表达式
        Expr fallback = parseExpression(getPrecedence() - 1); // 右结合

        // 创建 Elvis 操作符表达式
        return new ElvisExpr(left, fallback,
                new SourceLocation(left.getLocation().getStartLine(),
                        left.getLocation().getStartColumn(),
                        fallback.getLocation().getEndLine(),
                        fallback.getLocation().getEndColumn()));


    }

    /**
     * 解析范围表达式
     *
     * @param left  左操作数
     * @param token 词法单元
     * @return 表达式节点
     */
    public Expr parseRange(Expr left, Token token) {
        // 解析结束表达式
        Expr end = parseExpression(Precedence.RANGE - 1);

        // 创建范围表达式
        boolean inclusive = token.getType() == TokenType.RANGE;
        return new RangeExpr(left, end, inclusive,
                new SourceLocation(left.getLocation().getStartLine(),
                        left.getLocation().getStartColumn(),
                        end.getLocation().getEndLine(),
                        end.getLocation().getEndColumn()));

    }

    /**
     * 优先级常量
     */
    public static class Precedence {
        public static final int NONE = 0;
        public static final int ASSIGNMENT = 10;      // =
        public static final int CONDITIONAL = 20;     // ?:
        public static final int LOGICAL_OR = 30;      // ||
        public static final int LOGICAL_AND = 40;     // &&
        public static final int EQUALITY = 50;        // == !=
        public static final int COMPARISON = 60;      // < > <= >=
        public static final int TERM = 70;            // + -
        public static final int FACTOR = 80;          // * / %
        public static final int UNARY = 90;           // ! -
        public static final int CALL = 100;           // . ?. ()
        public static final int PRIMARY = 110;        // literals, identifiers
        public static final int RANGE = 120;          // .. ..<
    }
}