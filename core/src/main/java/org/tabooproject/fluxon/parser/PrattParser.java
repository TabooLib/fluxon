package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.expression.ContextCallExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.operator.InfixOperator;
import org.tabooproject.fluxon.parser.operator.PrefixOperator;
import org.tabooproject.fluxon.parser.type.BlockParser;
import org.tabooproject.fluxon.parser.type.FunctionCallParser;
import org.tabooproject.fluxon.parser.type.PostfixParser;

import java.util.ArrayList;

/**
 * Pratt Parser 核心实现
 * <p>
 * 使用 Top-Down Operator Precedence (Pratt Parsing) 算法处理运算符优先级。
 * 通过 {@link OperatorRegistry} 动态查找运算符，支持扩展。
 *
 * <h3>算法概述</h3>
 * <ol>
 *   <li>解析前缀表达式（包括 primary）</li>
 *   <li>循环检查中缀运算符，如果绑定力 >= 最小绑定力则继续解析</li>
 *   <li>递归解析右侧表达式，根据结合性决定绑定力</li>
 * </ol>
 */
public class PrattParser {

    /**
     * 解析表达式（入口）
     */
    public static ParseResult parse(Parser parser) {
        return Trampoline.run(parseExpression(parser, 0, Trampoline::done));
    }

    /**
     * 解析表达式（CPS 版本）
     *
     * @param parser          解析器
     * @param minBindingPower 最小绑定力
     * @param continuation    继续函数
     * @return 解析结果
     */
    public static Trampoline<ParseResult> parseExpression(Parser parser, int minBindingPower, Trampoline.Continuation<ParseResult> continuation) {
        return Trampoline.more(() -> parsePrefix(parser, left -> parseInfixLoop(parser, left, minBindingPower, continuation)));
    }

    /**
     * 解析前缀表达式
     * <p>
     * 先检查前缀运算符，没有则解析 primary 和调用表达式。
     */
    public static Trampoline<ParseResult> parsePrefix(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        OperatorRegistry registry = parser.getContext().getOperatorRegistry();
        PrefixOperator prefixOp = registry.findPrefixMatch(parser);
        if (prefixOp != null) {
            return prefixOp.parse(parser, continuation);
        }
        // 没有前缀运算符，解析 primary 然后处理调用
        return parseCallablePrimary(parser, expr -> parseCallExpression(parser, expr, continuation));
    }

    /**
     * 循环解析中缀运算符
     */
    private static Trampoline<ParseResult> parseInfixLoop(Parser parser, ParseResult left, int minBindingPower, Trampoline.Continuation<ParseResult> continuation) {
        OperatorRegistry registry = parser.getContext().getOperatorRegistry();
        InfixOperator infixOp = registry.findInfixMatch(parser, minBindingPower);
        if (infixOp == null) {
            return continuation.apply(left);
        }
        Token operator = parser.consume();
        return infixOp.parse(parser, left, operator, newLeft -> parseInfixLoop(parser, newLeft, minBindingPower, continuation));
    }

    /**
     * 解析可调用的 primary
     */
    private static Trampoline<ParseResult> parseCallablePrimary(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
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
     * 解析 primary 表达式（入口）
     */
    public static ParseResult parsePrimary(Parser parser) {
        return Trampoline.run(parsePrimary(parser, Trampoline::done));
    }

    /**
     * 解析 primary 表达式（委托给 SyntaxMacroRegistry）
     */
    public static Trampoline<ParseResult> parsePrimary(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        SyntaxMacroRegistry registry = parser.getContext().getSyntaxMacroRegistry();
        SyntaxMacro macro = registry.findMatch(parser);
        if (macro != null) {
            return macro.parse(parser, continuation);
        }
        Token token = parser.peek();
        if (token.getType() == TokenType.EOF) {
            throw parser.createParseException("Eof", token);
        }
        throw parser.createParseException("Expected expression", token);
    }

    /**
     * 解析调用表达式（处理 :: 上下文调用）
     */
    public static Trampoline<ParseResult> parseCallExpression(Parser parser, ParseResult expr, Trampoline.Continuation<ParseResult> continuation) {
        if (parser.match(TokenType.CONTEXT_CALL)) {
            return handleContextCall(parser, expr, continuation);
        }
        return Trampoline.more(() -> continuation.apply(PostfixParser.parsePostfixOperations(parser, expr)));
    }

    /**
     * 处理 :: 上下文调用链
     */
    private static Trampoline<ParseResult> handleContextCall(Parser parser, ParseResult expr, Trampoline.Continuation<ParseResult> continuation) {
        Token contextCallToken = parser.previous();
        ParseResult context;
        if (parser.match(TokenType.LEFT_BRACE)) {
            boolean isContextCall = parser.getSymbolEnvironment().isContextCall();
            parser.getSymbolEnvironment().setContextCall(true);
            return BlockParser.parse(parser, block -> {
                parser.getSymbolEnvironment().setContextCall(isContextCall);
                ParseResult combined = parser.attachSource(new ContextCallExpression(expr, block), contextCallToken);
                return parseCallExpression(parser, combined, continuation);
            });
        } else {
            SymbolEnvironment env = parser.getSymbolEnvironment();
            boolean isContextCall = env.isContextCall();
            env.setContextCall(true);
            context = FunctionCallParser.parse(parser);
            env.setContextCall(isContextCall);
        }
        ParseResult combined = parser.attachSource(new ContextCallExpression(expr, context), contextCallToken);
        return parseCallExpression(parser, combined, continuation);
    }

    /**
     * 解析函数调用参数列表
     */
    private static ParseResult[] parseCallArguments(Parser parser) {
        ArrayList<ParseResult> arguments = new ArrayList<>();
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(parse(parser));
            } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RIGHT_PAREN));
        }
        parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
        return arguments.toArray(new ParseResult[0]);
    }
}
