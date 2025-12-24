package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.StatementMacro;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.type.FunctionDefinitionParser;

import java.util.List;

/**
 * 函数定义语句宏
 * <p>
 * 处理 sync def、async def、def 函数定义。
 * 仅支持顶层语句。
 */
public class FunctionDefinitionMacro implements StatementMacro {

    @Override
    public boolean matchesTopLevel(Parser parser) {
        return parser.checkAny(TokenType.SYNC, TokenType.ASYNC, TokenType.DEF);
    }

    @Override
    public ParseResult parseTopLevel(Parser parser, List<Annotation> annotations) {
        if (parser.match(TokenType.SYNC)) {
            parser.match(TokenType.DEF);
            return FunctionDefinitionParser.parse(parser, false, true, annotations);
        }
        if (parser.match(TokenType.ASYNC)) {
            parser.match(TokenType.DEF);
            return FunctionDefinitionParser.parse(parser, true, false, annotations);
        }
        if (parser.match(TokenType.DEF)) {
            return FunctionDefinitionParser.parse(parser, false, false, annotations);
        }
        throw parser.createParseException("Expected function definition", parser.peek());
    }

    @Override
    public int priority() {
        return 200;
    }
}
