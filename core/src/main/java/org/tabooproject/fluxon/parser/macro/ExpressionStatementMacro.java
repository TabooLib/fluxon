package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.StatementMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

import java.util.List;

/**
 * 表达式语句宏
 * <p>
 * 处理表达式语句，作为兜底处理。
 * 优先级最低，当没有其他语句宏匹配时使用。
 */
public class ExpressionStatementMacro implements StatementMacro {

    @Override
    public boolean matchesTopLevel(Parser parser) {
        // 兜底，总是匹配
        return true;
    }

    @Override
    public boolean matchesSub(Parser parser) {
        // 兜底，总是匹配
        return true;
    }

    @Override
    public ParseResult parseTopLevel(Parser parser, List<Annotation> annotations) {
        if (!annotations.isEmpty()) {
            throw new RuntimeException("Annotations can only be applied to function definitions");
        }
        ParseResult expr = ExpressionParser.parse(parser);
        parser.match(TokenType.SEMICOLON);
        return parser.copySource(new ExpressionStatement(expr), expr);
    }

    @Override
    public Trampoline<ParseResult> parseSub(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        return ExpressionParser.parse(parser, expr -> {
            parser.match(TokenType.SEMICOLON);
            return continuation.apply(parser.copySource(new ExpressionStatement(expr), expr));
        });
    }

    @Override
    public int priority() {
        return 0; // 最低优先级，兜底
    }
}
