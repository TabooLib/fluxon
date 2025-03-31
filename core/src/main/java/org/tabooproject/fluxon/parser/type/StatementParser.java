package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.statement.BreakStatement;
import org.tabooproject.fluxon.parser.statement.ContinueStatement;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;

public class StatementParser {

    /**
     * 解析语句
     *
     * @return 解析结果
     */
    public static ParseResult parse(Parser parser) {
        // 检查异步函数定义
        if (parser.check(TokenType.ASYNC) && parser.peek(1).is(TokenType.DEF)) {
            parser.advance(); // 消费 ASYNC
            parser.advance(); // 消费 DEF
            return FunctionDefinitionParser.parse(parser, true);
        }

        // 检查特殊语法
        TokenType match = parser.match(TokenType.DEF, TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE);
        if (match != null) {
            switch (match) {
                // 普通函数定义
                case DEF:
                    return FunctionDefinitionParser.parse(parser, false);
                // 返回值
                case RETURN: {
                    // 检查是否有返回值
                    if (parser.isEndOfExpression()) {
                        parser.match(TokenType.SEMICOLON); // 可选的分号
                        return new ReturnStatement(null);
                    }
                    ParseResult returnValue = ExpressionParser.parse(parser);
                    parser.match(TokenType.SEMICOLON); // 可选的分号
                    return new ReturnStatement(returnValue);
                }
                // 跳出循环
                case BREAK: {
                    // 是否允许跳出循环
                    if (parser.getCurrentScope().isBreakable()) {
                        parser.match(TokenType.SEMICOLON); // 可选的分号
                        return new BreakStatement();
                    }
                    throw new RuntimeException("Break outside of loop");
                }
                // 继续循环
                case CONTINUE: {
                    // 是否允许继续循环
                    if (parser.getCurrentScope().isContinuable()) {
                        parser.match(TokenType.SEMICOLON); // 可选的分号
                        return new ContinueStatement();
                    }
                    throw new RuntimeException("Continue outside of loop");
                }
                default:
            }
        }

        // 解析表达式语句
        ParseResult expr = ExpressionParser.parse(parser);
        parser.match(TokenType.SEMICOLON); // 可选的分号
        return new ExpressionStatement(expr);
    }
}
