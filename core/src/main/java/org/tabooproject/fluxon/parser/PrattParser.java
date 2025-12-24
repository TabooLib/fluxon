package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.type.PostfixParser;

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
     * 先检查前缀运算符，没有则解析 primary 和后缀操作。
     */
    private static Trampoline<ParseResult> parsePrefix(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        OperatorRegistry registry = parser.getContext().getOperatorRegistry();
        PrefixOperator prefixOp = registry.findPrefixMatch(parser);
        if (prefixOp != null) {
            return prefixOp.parse(parser, continuation);
        }
        // 没有前缀运算符，解析 primary 然后处理后缀操作
        return parsePrimary(parser, primary -> {
            ParseResult expr = PostfixParser.parsePostfixOperations(parser, primary);
            return Trampoline.more(() -> continuation.apply(expr)); // 使用 Trampoline.more 包装，避免栈溢出
        });
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
}
