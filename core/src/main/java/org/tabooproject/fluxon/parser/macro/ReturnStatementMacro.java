package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.StatementMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

import java.util.List;

/**
 * Return 语句宏
 * <p>
 * 处理 return 语句，支持顶层和子语句。
 */
public class ReturnStatementMacro implements StatementMacro {

    @Override
    public boolean matchesTopLevel(Parser parser) {
        return parser.check(TokenType.RETURN);
    }

    @Override
    public boolean matchesSub(Parser parser) {
        return parser.check(TokenType.RETURN);
    }

    @Override
    public ParseResult parseTopLevel(Parser parser, List<Annotation> annotations) {
        if (!annotations.isEmpty()) {
            throw new RuntimeException("Annotations cannot be applied to return statements");
        }
        Token returnToken = parser.consume(TokenType.RETURN, "Expected 'return'");
        if (parser.isEndOfExpression()) {
            parser.match(TokenType.SEMICOLON);
            return parser.attachSource(new ReturnStatement(null), returnToken);
        }
        ParseResult returnValue = ExpressionParser.parse(parser);
        parser.match(TokenType.SEMICOLON);
        return parser.attachSource(new ReturnStatement(returnValue), returnToken);
    }

    @Override
    public Trampoline<ParseResult> parseSub(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token returnToken = parser.consume(TokenType.RETURN, "Expected 'return'");
        if (parser.isEndOfExpression()) {
            parser.match(TokenType.SEMICOLON);
            return continuation.apply(parser.attachSource(new ReturnStatement(null), returnToken));
        }
        return ExpressionParser.parse(parser, returnValue -> {
            parser.match(TokenType.SEMICOLON);
            return continuation.apply(parser.attachSource(new ReturnStatement(returnValue), returnToken));
        });
    }

    @Override
    public int priority() {
        return 100;
    }
}
