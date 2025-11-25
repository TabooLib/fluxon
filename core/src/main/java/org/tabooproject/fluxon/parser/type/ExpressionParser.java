package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SourceExcerpt;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.error.VariableNotFoundException;
import org.tabooproject.fluxon.parser.expression.*;
import org.tabooproject.fluxon.parser.expression.literal.*;

import java.util.ArrayList;
import java.util.function.Supplier;

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

    private interface ExpressionContinuation {
        Trampoline<ParseResult> apply(ParseResult value);
    }

    private interface Trampoline<T> {
        T get();

        default boolean isComplete() {
            return true;
        }

        default Trampoline<T> next() {
            throw new UnsupportedOperationException("Trampoline is already complete");
        }

        static <T> Trampoline<T> done(T value) {
            return () -> value;
        }

        static <T> Trampoline<T> more(Supplier<Trampoline<T>> supplier) {
            return new Trampoline<T>() {
                @Override
                public boolean isComplete() {
                    return false;
                }

                @Override
                public T get() {
                    throw new IllegalStateException("Trampoline has not finished");
                }

                @Override
                public Trampoline<T> next() {
                    return supplier.get();
                }
            };
        }

        static <T> T run(Trampoline<T> trampoline) {
            Trampoline<T> current = trampoline;
            while (!current.isComplete()) {
                current = current.next();
            }
            return current.get();
        }
    }

    private static final class AssignmentTarget {
        private final ParseResult expression;
        private final int position;

        private AssignmentTarget(ParseResult expression, int position) {
            this.expression = expression;
            this.position = position;
        }

        public ParseResult expression() {
            return expression;
        }

        public int position() {
            return position;
        }
    }

    /**
     * 解析语句
     *
     * @return 解析结果
     */
    public static ParseResult parse(Parser parser) {
        return Trampoline.run(parseExpression(parser, Trampoline::done));
    }

    public static ParseResult parsePrimary(Parser parser) {
        return Trampoline.run(parsePrimary(parser, Trampoline::done));
    }

    private static Trampoline<ParseResult> parseExpression(Parser parser, ExpressionContinuation continuation) {
        return parseAssignment(parser, continuation);
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
    private static Trampoline<ParseResult> parseAssignment(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseElvis(parser, left -> {
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
                AssignmentTarget target = prepareAssignmentTarget(parser, match, left, operator);
                return parseAssignment(parser, right -> continuation.apply(new AssignExpression(target.expression(), operator, right, target.position())));
            }
            return continuation.apply(left);
        }));
    }

    /**
     * 解析三元运算符表达式
     * 三元运算符 condition ? true_expr : false_expr
     *
     * @return 三元运算符表达式解析结果
     */
    private static Trampoline<ParseResult> parseTernary(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseLogicalOr(parser, condition -> {
            if (parser.match(TokenType.QUESTION)) {
                return parseTernary(parser, trueExpr -> {
                    parser.consume(TokenType.COLON, "Expected ':' after ternary true expression");
                    return parseTernary(parser, falseExpr -> continuation.apply(new TernaryExpression(condition, trueExpr, falseExpr)));
                });
            }
            return continuation.apply(condition);
        }));
    }

    /**
     * 解析Elvis操作符表达式
     * Elvis操作符 ?: 用于提供默认值，例如：a ?: b 表示如果a为null则返回b，否则返回a
     *
     * @return Elvis操作符表达式解析结果
     */
    private static Trampoline<ParseResult> parseElvis(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseTernary(parser, expr -> {
            if (parser.match(TokenType.QUESTION_COLON)) {
                return parseElvis(parser, right -> continuation.apply(new ElvisExpression(expr, right)));
            }
            return continuation.apply(expr);
        }));
    }

    /**
     * 解析逻辑或表达式
     *
     * @return 逻辑或表达式解析结果
     */
    private static Trampoline<ParseResult> parseLogicalOr(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseLogicalAnd(parser, left -> parseLogicalOrRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseLogicalOrRest(Parser parser, ParseResult left, ExpressionContinuation continuation) {
        if (parser.match(TokenType.OR)) {
            Token operator = parser.previous();
            return parseLogicalAnd(parser, right -> parseLogicalOrRest(parser, new LogicalExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析逻辑与表达式
     *
     * @return 逻辑与表达式解析结果
     */
    private static Trampoline<ParseResult> parseLogicalAnd(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseRange(parser, left -> parseLogicalAndRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseLogicalAndRest(Parser parser, ParseResult left, ExpressionContinuation continuation) {
        if (parser.match(TokenType.AND)) {
            Token operator = parser.previous();
            return parseRange(parser, right -> parseLogicalAndRest(parser, new LogicalExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析范围表达式
     * 范围表达式用于创建一个范围，例如：1..10 表示从1到10的范围（包含10）
     * 1..<10 表示从1到9的范围（不包含10）
     *
     * @return 范围表达式解析结果
     */
    private static Trampoline<ParseResult> parseRange(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseEquality(parser, left -> {
            TokenType match = parser.match(TokenType.RANGE, TokenType.RANGE_EXCLUSIVE);
            if (match != null) {
                return parseEquality(parser, right -> continuation.apply(new RangeExpression(left, right, match == TokenType.RANGE)));
            }
            return continuation.apply(left);
        }));
    }

    /**
     * 解析相等性表达式（等于、不等于）
     *
     * @return 相等性表达式解析结果
     */
    private static Trampoline<ParseResult> parseEquality(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseComparison(parser, left -> parseEqualityRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseEqualityRest(Parser parser, ParseResult left, ExpressionContinuation continuation) {
        TokenType match = parser.match(TokenType.EQUAL, TokenType.NOT_EQUAL);
        if (match != null) {
            Token operator = parser.previous();
            return parseComparison(parser, right -> parseEqualityRest(parser, new BinaryExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析比较表达式（大于、大于等于、小于、小于等于）
     *
     * @return 比较表达式解析结果
     */
    private static Trampoline<ParseResult> parseComparison(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseTerm(parser, left -> parseComparisonRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseComparisonRest(Parser parser, ParseResult left, ExpressionContinuation continuation) {
        TokenType match = parser.match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL);
        if (match != null) {
            Token operator = parser.previous();
            return parseTerm(parser, right -> parseComparisonRest(parser, new BinaryExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析项表达式（加、减）
     *
     * @return 项表达式解析结果
     */
    private static Trampoline<ParseResult> parseTerm(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseFactor(parser, left -> parseTermRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseTermRest(Parser parser, ParseResult left, ExpressionContinuation continuation) {
        TokenType match = parser.match(TokenType.PLUS, TokenType.MINUS);
        if (match != null) {
            Token operator = parser.previous();
            return parseFactor(parser, right -> parseTermRest(parser, new BinaryExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析因子表达式（乘、除、取模）
     *
     * @return 因子表达式解析结果
     */
    private static Trampoline<ParseResult> parseFactor(Parser parser, ExpressionContinuation continuation) {
        return Trampoline.more(() -> parseUnary(parser, left -> parseFactorRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseFactorRest(Parser parser, ParseResult left, ExpressionContinuation continuation) {
        TokenType match = parser.match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO);
        if (match != null) {
            Token operator = parser.previous();
            return parseUnary(parser, right -> parseFactorRest(parser, new BinaryExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析一元表达式（负号、逻辑非）
     *
     * @return 一元表达式解析结果
     */
    private static Trampoline<ParseResult> parseUnary(Parser parser, ExpressionContinuation continuation) {
        switch (parser.peek().getType()) {
            case NOT:
            case MINUS: {
                Token operator = parser.consume();
                return parseUnary(parser, expr -> continuation.apply(new UnaryExpression(operator, expr)));
            }
            case AWAIT: {
                parser.consume();
                return parseUnary(parser, expr -> continuation.apply(new AwaitExpression(expr)));
            }
            default:
                return parseReference(parser, continuation);
        }
    }

    /**
     * 解析引用表达式（&）
     * 引用的优先级高于一元操作符，低于调用操作
     *
     * @return 引用表达式解析结果
     */
    private static Trampoline<ParseResult> parseReference(Parser parser, ExpressionContinuation continuation) {
        if (parser.peek().getType() == TokenType.AMPERSAND) {
            parser.consume();
            boolean isOptional = parser.match(TokenType.QUESTION);
            String name = parser.consume(TokenType.IDENTIFIER, "Expect variable name after '&'.").getLexeme();
            ParseResult ref;
            if (isOptional) {
                ref = new ReferenceExpression(new Identifier(name), true, -1);
            } else {
                Integer captured = parser.getCapturedIndex(name);
                if (captured != null) {
                    ref = new ReferenceExpression(new Identifier(name), false, captured);
                } else if (parser.getContext().isAllowInvalidReference() || parser.isFunction(name) || parser.hasVariable(name)) {
                    ref = new ReferenceExpression(new Identifier(name), parser.getContext().isAllowInvalidReference(), parser.getSymbolEnvironment().getLocalVariable(name));
                } else {
                    Token token = parser.peek();
                    SourceExcerpt excerpt = SourceExcerpt.from(parser.getContext(), token);
                    throw new VariableNotFoundException(name, new ArrayList<>(parser.getSymbolEnvironment().getLocalVariables().keySet()), token, excerpt);
                }
            }
            ref = PostfixParser.parsePostfixOperations(parser, ref);
            return parseCallExpression(parser, ref, continuation);
        }
        return Trampoline.more(() -> parseCallExpression(parser, FunctionCallParser.parse(parser), continuation));
    }

    /**
     * 解析调用表达式（处理已有表达式的后续调用操作）
     *
     * @param parser 解析器
     * @param expr   已解析的表达式
     * @return 应用了调用操作的表达式
     */
    private static Trampoline<ParseResult> parseCallExpression(Parser parser, ParseResult expr, ExpressionContinuation continuation) {
        if (parser.match(TokenType.CONTEXT_CALL)) {
            return handleContextCall(parser, expr, continuation);
        }
        return Trampoline.more(() -> continuation.apply(PostfixParser.parsePostfixOperations(parser, expr)));
    }

    private static Trampoline<ParseResult> handleContextCall(Parser parser, ParseResult expr, ExpressionContinuation continuation) {
        ParseResult context;
        if (parser.match(TokenType.LEFT_BRACE)) {
            boolean isContextCall = parser.getSymbolEnvironment().isContextCall();
            parser.getSymbolEnvironment().setContextCall(true);
            context = BlockParser.parse(parser);
            parser.getSymbolEnvironment().setContextCall(isContextCall);
        } else {
            SymbolEnvironment env = parser.getSymbolEnvironment();
            boolean isContextCall = env.isContextCall();
            env.setContextCall(true);
            context = FunctionCallParser.parse(parser);
            env.setContextCall(isContextCall);
        }
        ParseResult combined = new ContextCallExpression(expr, context);
        return parseCallExpression(parser, combined, continuation);
    }

    /**
     * 解析基本表达式
     *
     * @return 基本表达式解析结果
     */
    private static Trampoline<ParseResult> parsePrimary(Parser parser, ExpressionContinuation continuation) {
        switch (parser.peek().getType()) {
            case IDENTIFIER:
                return Trampoline.more(() -> continuation.apply(new Identifier(parser.consume().getLexeme())));

            case STRING:
                return Trampoline.more(() -> continuation.apply(new StringLiteral(parser.consume().getLexeme())));
            case INTEGER:
                return Trampoline.more(() -> continuation.apply(new IntLiteral((int) parser.consume().getValue())));
            case LONG:
                return Trampoline.more(() -> continuation.apply(new LongLiteral((long) parser.consume().getValue())));
            case FLOAT:
                return Trampoline.more(() -> continuation.apply(new FloatLiteral((float) parser.consume().getValue())));
            case DOUBLE:
                return Trampoline.more(() -> continuation.apply(new DoubleLiteral((double) parser.consume().getValue())));
            case TRUE: {
                parser.consume();
                return Trampoline.more(() -> continuation.apply(new BooleanLiteral(true)));
            }
            case FALSE: {
                parser.consume();
                return Trampoline.more(() -> continuation.apply(new BooleanLiteral(false)));
            }
            case NULL:
                parser.consume();
                return Trampoline.more(() -> continuation.apply(new NullLiteral()));

            case LEFT_BRACKET: {
                parser.consume();
                return Trampoline.more(() -> continuation.apply(ListParser.parse(parser)));
            }

            case IF:
                return Trampoline.more(() -> continuation.apply(IfParser.parse(parser)));
            case FOR:
                return Trampoline.more(() -> continuation.apply(ForParser.parse(parser)));
            case WHEN:
                return Trampoline.more(() -> continuation.apply(WhenParser.parse(parser)));
            case WHILE:
                return Trampoline.more(() -> continuation.apply(WhileParser.parse(parser)));
            case TRY:
                return Trampoline.more(() -> continuation.apply(TryParser.parse(parser)));
            case PIPE:
            case OR:
                return Trampoline.more(() -> continuation.apply(LambdaParser.parse(parser)));

            case LEFT_PAREN: {
                parser.consume();
                return parseExpression(parser, expr -> {
                    parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
                    return continuation.apply(new GroupingExpression(expr));
                });
            }
            case LEFT_BRACE: {
                parser.consume();
                return Trampoline.more(() -> continuation.apply(BlockParser.parse(parser)));
            }
            case EOF:
                throw parser.createParseException("Eof", parser.peek());
            default:
                throw parser.createParseException("Expected expression", parser.peek());
        }
    }

    private static AssignmentTarget prepareAssignmentTarget(Parser parser, TokenType match, ParseResult expr, Token operator) {
        ParseResult target = expr;
        if (target instanceof ReferenceExpression) {
            target = ((ReferenceExpression) target).getIdentifier();
        }
        if (target instanceof Identifier) {
            String name = ((Identifier) target).getValue();
            int position = parser.getSymbolEnvironment().getLocalVariable(name);
            Integer captured = parser.getCapturedIndex(name);
            if (captured != null) {
                position = captured;
            }
            if (match == TokenType.ASSIGN && captured == null && !parser.getSymbolEnvironment().hasVariable(name)) {
                parser.defineVariable(name);
                position = parser.getSymbolEnvironment().getLocalVariable(name);
            }
            return new AssignmentTarget(target, position);
        } else if (target instanceof IndexAccessExpression) {
            return new AssignmentTarget(target, -1);
        }
        throw parser.createParseException("Invalid assignment target: " + target, operator);
    }
}

