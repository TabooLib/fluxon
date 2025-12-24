package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.StatementMacro;
import org.tabooproject.fluxon.parser.StatementMacroRegistry;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;

import java.util.Collections;

/**
 * 语句解析器 - 委托给 StatementMacroRegistry
 * <p>
 * 通过 {@link StatementMacroRegistry} 动态查找语句宏进行解析。
 *
 * @see StatementMacro
 * @see StatementMacroRegistry
 */
public class StatementParser {

    /**
     * 解析顶层语句
     */
    public static ParseResult parseTopLevel(Parser parser) {
        StatementMacroRegistry registry = parser.getContext().getStatementMacroRegistry();
        StatementMacro macro = registry.findTopLevelMatch(parser);
        if (macro != null) {
            return macro.parseTopLevel(parser);
        }
        throw parser.createParseException("Expected statement", parser.peek());
    }

    /**
     * 解析子语句（CPS 版本）
     */
    public static Trampoline<ParseResult> parseSub(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        StatementMacroRegistry registry = parser.getContext().getStatementMacroRegistry();
        StatementMacro macro = registry.findSubMatch(parser);
        if (macro != null) {
            return macro.parseSub(parser, continuation);
        }
        throw parser.createParseException("Expected statement", parser.peek());
    }

    /**
     * 解析子语句，如果是 ExpressionStatement 则提取其表达式部分
     */
    public static Trampoline<ParseResult> parseSubToExpr(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        return parseSub(parser, arm -> {
            ParseResult normalized = arm;
            if (arm instanceof ExpressionStatement) {
                normalized = ((ExpressionStatement) arm).getExpression();
            }
            return continuation.apply(normalized);
        });
    }
}
