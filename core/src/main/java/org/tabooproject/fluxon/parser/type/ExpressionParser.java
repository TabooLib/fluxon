package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.parser.*;

/**
 * 表达式解析器 - 委托给 Pratt Parser
 * <p>
 * 这是一个门面类，所有实际解析逻辑都在 {@link PrattParser} 中。
 *
 * @see PrattParser
 * @see OperatorRegistry
 * @see SyntaxMacroRegistry
 */
public class ExpressionParser {

    /**
     * 解析表达式（入口）
     */
    public static ParseResult parse(Parser parser) {
        return PrattParser.parse(parser);
    }

    /**
     * 解析表达式（CPS 版本）
     */
    public static Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        return PrattParser.parseExpression(parser, 0, continuation);
    }

    /**
     * 解析 primary 表达式（入口）
     */
    public static ParseResult parsePrimary(Parser parser) {
        return PrattParser.parsePrimary(parser);
    }

    /**
     * 解析 primary 表达式（CPS 版本）
     */
    public static Trampoline<ParseResult> parsePrimary(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        return PrattParser.parsePrimary(parser, continuation);
    }
}