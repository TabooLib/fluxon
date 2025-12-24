package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.StatementMacro;
import org.tabooproject.fluxon.parser.StatementMacroRegistry;

import java.util.ArrayList;
import java.util.List;

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
     * <p>
     * 处理注解、函数定义、return 语句和表达式语句。
     */
    public static ParseResult parseTopLevel(Parser parser) {
        // 收集注解
        List<Annotation> annotations = new ArrayList<>();
        while (parser.check(TokenType.AT)) {
            annotations.add(AnnotationParser.parse(parser));
        }
        // 查找匹配的语句宏
        StatementMacroRegistry registry = parser.getContext().getStatementMacroRegistry();
        StatementMacro macro = registry.findTopLevelMatch(parser);
        if (macro != null) {
            return macro.parseTopLevel(parser, annotations);
        }
        // 如果有注解但没有匹配的宏，报错
        if (!annotations.isEmpty()) {
            throw new RuntimeException("Annotations can only be applied to function definitions");
        }
        throw parser.createParseException("Expected statement", parser.peek());
    }

    /**
     * 解析子语句（CPS 版本）
     * <p>
     * 处理 return、break、continue 和表达式语句。
     */
    public static Trampoline<ParseResult> parseSub(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        StatementMacroRegistry registry = parser.getContext().getStatementMacroRegistry();
        StatementMacro macro = registry.findSubMatch(parser);
        if (macro != null) {
            return macro.parseSub(parser, continuation);
        }
        throw parser.createParseException("Expected statement", parser.peek());
    }
}
