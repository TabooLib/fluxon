package org.tabooproject.fluxon.parser.impl;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.statements.Statements;

public class StatementParser {

    /**
     * 解析语句
     *
     * @return 解析结果
     */
    public static ParseResult parse(Parser parser) {
        // 检查函数定义
        if (parser.check(TokenType.ASYNC) && parser.peek(1).is(TokenType.DEF)) {
            // 异步函数定义
            parser.advance(); // 消费 ASYNC
            parser.advance(); // 消费 DEF
            return FunctionDefinitionParser.parse(parser, true);
        }
        // 普通函数定义
        else if (parser.match(TokenType.DEF)) {
            return FunctionDefinitionParser.parse(parser, false);
        }
        // 解析表达式语句
        ParseResult expr = ExpressionParser.parse(parser);
        // 可选的分号
        parser.match(TokenType.SEMICOLON);
        return new Statements.ExpressionStatement(expr);
    }
}
