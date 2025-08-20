package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.expression.TryExpression;

public class TryParser {

    /**
     * 解析 try 表达式
     * 语法：try { ... } [catch (var) { ... }] [finally { ... }]
     *
     * @return try 表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        // 消费 TRY 标记
        parser.consume(TokenType.TRY, "Expected 'try' before try expression");
        
        // 解析 try 块（大括号可选）
        ParseResult tryBody;
        if (parser.match(TokenType.LEFT_BRACE)) {
            tryBody = BlockParser.parse(parser);
        } else {
            tryBody = ExpressionParser.parse(parser);
        }
        
        // 可选的 catch 块
        String catchVarName = null;
        int position = -1;
        ParseResult catchBody = null;
        if (parser.match(TokenType.CATCH)) {
            // 解析可选的 catch (变量名) 或直接是 catch 块
            if (parser.match(TokenType.LEFT_PAREN)) {
                // 有括号，解析变量名
                catchVarName = parser.consume(TokenType.IDENTIFIER, "Expected variable name in catch clause").getLexeme();
                parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after catch variable");
                // 在 catch 块中添加异常变量到符号环境
                SymbolEnvironment env = parser.getSymbolEnvironment();
                env.defineVariable(catchVarName);
                position = env.getLocalVariable(catchVarName);
            }

            // 解析 catch 块（大括号可选）
            if (parser.match(TokenType.LEFT_BRACE)) {
                catchBody = BlockParser.parse(parser);
            } else {
                catchBody = ExpressionParser.parse(parser);
            }
        }
        
        // 可选的 finally 块
        ParseResult finallyBody = null;
        if (parser.match(TokenType.FINALLY)) {
            // 解析 finally 块（大括号可选）
            if (parser.match(TokenType.LEFT_BRACE)) {
                finallyBody = BlockParser.parse(parser);
            } else {
                finallyBody = ExpressionParser.parse(parser);
            }
        }
        return new TryExpression(tryBody, catchVarName, position, catchBody, finallyBody);
    }
}
