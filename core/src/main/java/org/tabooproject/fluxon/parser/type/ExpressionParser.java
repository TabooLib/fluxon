package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.error.VariableNotFoundException;
import org.tabooproject.fluxon.parser.expression.*;
import org.tabooproject.fluxon.parser.expression.literal.*;

import java.util.ArrayList;

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
 * 为消除深度嵌套导致的调用栈溢出，解析流程改为 CPS + trampoline：每个阶段接受 continuation，将后续工作封装为惰性 Trampoline
 * 再由 Trampoline.run 以循环驱动，这样保持原有优先级与错误路径，同时不会递归消耗 JVM 栈。
 */
public class ExpressionParser {

    /**
     * 赋值目标
     */
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
     * <p>
     * 入口：启动 trampoline，按优先级链条解析完整表达式。
     *
     * @return 解析结果
     */
    public static ParseResult parse(Parser parser) {
        return Trampoline.run(parseExpression(parser, Trampoline::done));
    }

    public static ParseResult parsePrimary(Parser parser) {
        return Trampoline.run(parsePrimary(parser, Trampoline::done));
    }

    private static Trampoline<ParseResult> parseExpression(Parser parser, TrampolineContinuation continuation) {
        return parseAssignment(parser, continuation);
    }

    /**
     * 解析赋值表达式
     * <p>
     * 为什么要先解析逻辑或表达式，然后再解析赋值表达式？
     * 在解析器设计中，通常会使用递归下降解析（Recursive Descent Parsing）的方法来处理不同优先级的表达式。
     * 即当解析某一层次的表达式时，它会先调用解析更高优先级表达式的方法，然后再处理当前层次的操作符。
     * 这种设计确保了表达式 a = b || c 被正确解析为 a = (b || c) 而不是 (a = b) || c。
     * <p>
     * 在 trampoline 中，先解析左侧，再决定是否继续解析右侧并构建 AssignExpression。
     *
     * @return 赋值表达式解析结果
     */
    private static Trampoline<ParseResult> parseAssignment(Parser parser, TrampolineContinuation continuation) {
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
    private static Trampoline<ParseResult> parseTernary(Parser parser, TrampolineContinuation continuation) {
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
     * 解析 Elvis 操作符表达式
     * Elvis 操作符 ?: 用于提供默认值，例如：a ?: b 表示如果 a 为 null 则返回 b，否则返回 a
     *
     * @return Elvis 操作符表达式解析结果
     */
    private static Trampoline<ParseResult> parseElvis(Parser parser, TrampolineContinuation continuation) {
        return Trampoline.more(() -> parseTernary(parser, expr -> {
            if (parser.match(TokenType.QUESTION_COLON)) {
                return parseElvis(parser, right -> continuation.apply(new ElvisExpression(expr, right)));
            }
            return continuation.apply(expr);
        }));
    }

    /**
     * 解析逻辑或表达式
     * <p>
     * 循环式展开：左侧完成后，如果继续匹配 OR，则构建新的 LogicalExpression 并继续。
     *
     * @return 逻辑或表达式解析结果
     */
    private static Trampoline<ParseResult> parseLogicalOr(Parser parser, TrampolineContinuation continuation) {
        return Trampoline.more(() -> parseLogicalAnd(parser, left -> parseLogicalOrRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseLogicalOrRest(Parser parser, ParseResult left, TrampolineContinuation continuation) {
        if (parser.match(TokenType.OR)) {
            Token operator = parser.previous();
            return parseLogicalAnd(parser, right -> parseLogicalOrRest(parser, new LogicalExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析逻辑与表达式
     * <p>
     * 与逻辑或相同，使用尾递归样式展开成 trampoline 链。
     *
     * @return 逻辑与表达式解析结果
     */
    private static Trampoline<ParseResult> parseLogicalAnd(Parser parser, TrampolineContinuation continuation) {
        return Trampoline.more(() -> parseRange(parser, left -> parseLogicalAndRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseLogicalAndRest(Parser parser, ParseResult left, TrampolineContinuation continuation) {
        if (parser.match(TokenType.AND)) {
            Token operator = parser.previous();
            return parseRange(parser, right -> parseLogicalAndRest(parser, new LogicalExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析范围表达式
     * 范围表达式用于创建一个范围，例如：1..10 表示从1到10的范围（包含 10）
     * 1..<10 表示从 1 到 9 的范围（不包含 10）
     * <p>
     * 按左结合构造 RangeExpression，并保持错误消息一致。
     *
     * @return 范围表达式解析结果
     */
    private static Trampoline<ParseResult> parseRange(Parser parser, TrampolineContinuation continuation) {
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
     * <p>
     * 循环匹配相等/不等，构建 BinaryExpression 链。
     *
     * @return 相等性表达式解析结果
     */
    private static Trampoline<ParseResult> parseEquality(Parser parser, TrampolineContinuation continuation) {
        return Trampoline.more(() -> parseComparison(parser, left -> parseEqualityRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseEqualityRest(Parser parser, ParseResult left, TrampolineContinuation continuation) {
        TokenType match = parser.match(TokenType.EQUAL, TokenType.NOT_EQUAL);
        if (match != null) {
            Token operator = parser.previous();
            return parseComparison(parser, right -> parseEqualityRest(parser, new BinaryExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析比较表达式（大于、大于等于、小于、小于等于）
     * <p>
     * 与相等性相同，尾式展开避免栈深。
     *
     * @return 比较表达式解析结果
     */
    private static Trampoline<ParseResult> parseComparison(Parser parser, TrampolineContinuation continuation) {
        return Trampoline.more(() -> parseTerm(parser, left -> parseComparisonRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseComparisonRest(Parser parser, ParseResult left, TrampolineContinuation continuation) {
        TokenType match = parser.match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL);
        if (match != null) {
            Token operator = parser.previous();
            return parseTerm(parser, right -> parseComparisonRest(parser, new BinaryExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析项表达式（加、减）
     * <p>
     * 与乘除、比较同样模式。
     *
     * @return 项表达式解析结果
     */
    private static Trampoline<ParseResult> parseTerm(Parser parser, TrampolineContinuation continuation) {
        return Trampoline.more(() -> parseFactor(parser, left -> parseTermRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseTermRest(Parser parser, ParseResult left, TrampolineContinuation continuation) {
        TokenType match = parser.match(TokenType.PLUS, TokenType.MINUS);
        if (match != null) {
            Token operator = parser.previous();
            return parseFactor(parser, right -> parseTermRest(parser, new BinaryExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析因子表达式（乘、除、取模）
     * <p>
     * 使用 trampoline 持续匹配 * / %，避免递归。
     *
     * @return 因子表达式解析结果
     */
    private static Trampoline<ParseResult> parseFactor(Parser parser, TrampolineContinuation continuation) {
        return Trampoline.more(() -> parseUnary(parser, left -> parseFactorRest(parser, left, continuation)));
    }

    private static Trampoline<ParseResult> parseFactorRest(Parser parser, ParseResult left, TrampolineContinuation continuation) {
        TokenType match = parser.match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO);
        if (match != null) {
            Token operator = parser.previous();
            return parseUnary(parser, right -> parseFactorRest(parser, new BinaryExpression(left, operator, right), continuation));
        }
        return continuation.apply(left);
    }

    /**
     * 解析一元表达式（负号、逻辑非）
     * <p>
     * 仍按优先级递进；遇到非/负号/await 时封装 continuation。
     *
     * @return 一元表达式解析结果
     */
    private static Trampoline<ParseResult> parseUnary(Parser parser, TrampolineContinuation continuation) {
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
    private static Trampoline<ParseResult> parseReference(Parser parser, TrampolineContinuation continuation) {
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
        return parseCallablePrimary(parser, expr -> parseCallExpression(parser, expr, continuation));
    }

    /**
     * 解析调用表达式（处理已有表达式的后续调用操作）
     *
     * @param parser 解析器
     * @param expr   已解析的表达式
     * @return 应用了调用操作的表达式
     */
    private static Trampoline<ParseResult> parseCallExpression(Parser parser, ParseResult expr, TrampolineContinuation continuation) {
        if (parser.match(TokenType.CONTEXT_CALL)) {
            return handleContextCall(parser, expr, continuation);
        }
        return Trampoline.more(() -> continuation.apply(PostfixParser.parsePostfixOperations(parser, expr)));
    }

    /**
     * 处理 :: 上下文调用链，保持原有符号环境切换与错误行为。
     */
    private static Trampoline<ParseResult> handleContextCall(Parser parser, ParseResult expr, TrampolineContinuation continuation) {
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
     * 解析可调用的 primary：仅对标识符尝试函数调用/顶层 context 调用，其余直接透传。
     * 这样避免再进入 FunctionCallParser 再 trampoline 进入 parsePrimary，减小堆栈链。
     */
    private static Trampoline<ParseResult> parseCallablePrimary(Parser parser, TrampolineContinuation continuation) {
        return parsePrimary(parser, primary -> {
            ParseResult expr = primary;
            if (expr instanceof Identifier) {
                Identifier callee = (Identifier) expr;
                if (parser.match(TokenType.LEFT_PAREN)) {
                    expr = FunctionCallParser.getFunctionCallExpression(parser, callee, parseCallArguments(parser));
                } else if (parser.check(TokenType.CONTEXT_CALL)) {
                    expr = FunctionCallParser.getFunctionCallExpression(parser, callee, new ParseResult[0]);
                }
            }
            expr = PostfixParser.parsePostfixOperations(parser, expr);
            return continuation.apply(expr);
        });
    }

    /**
     * 解析圆括号内的参数列表，保持与旧实现相同的消费顺序与错误信息。
     */
    private static ParseResult[] parseCallArguments(Parser parser) {
        ArrayList<ParseResult> arguments = new ArrayList<>();
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(ExpressionParser.parse(parser));
            } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RIGHT_PAREN));
        }
        parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
        return arguments.toArray(new ParseResult[0]);
    }

    /**
     * 解析基本表达式
     *
     * @return 基本表达式解析结果
     */
    private static Trampoline<ParseResult> parsePrimary(Parser parser, TrampolineContinuation continuation) {
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

    /**
     * 构建赋值目标信息，保持变量定义/捕获/非法目标的原有行为和错误消息。
     */
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