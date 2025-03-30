package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;

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
        // 解析 return 语句
        else if (parser.match(TokenType.RETURN)) {
            // 检查是否有返回值
            if (parser.isEndOfExpression()) {
                parser.match(TokenType.SEMICOLON); // 可选的分号
                return new ReturnStatement(null);
            }
            ParseResult returnValue = ExpressionParser.parse(parser);
            parser.match(TokenType.SEMICOLON); // 可选的分号
            return new ReturnStatement(returnValue);
        }
        // 解析表达式语句
        ParseResult expr = ExpressionParser.parse(parser);
        // 可选的分号
        parser.match(TokenType.SEMICOLON);
        return new ExpressionStatement(expr);
    }
}
