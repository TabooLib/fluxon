package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * 语法宏解析辅助工具
 */
public final class SyntaxMacroHelper {

    private SyntaxMacroHelper() {
    }

    /**
     * 解析全限定类名（标识符序列，用 . 连接）
     * <p>
     * 示例: java.util.ArrayList, java.lang.String
     *
     * @param parser       解析器实例
     * @param errorMessage 首个标识符缺失时的错误消息
     * @return 解析得到的全限定类名
     */
    public static String parseQualifiedName(Parser parser, String errorMessage) {
        StringBuilder className = new StringBuilder();
        Token firstIdentifier = parser.consume(TokenType.IDENTIFIER, errorMessage);
        className.append(firstIdentifier.getLexeme());
        // 继续解析 .identifier 序列
        while (parser.check(TokenType.DOT) && parser.peek(1).is(TokenType.IDENTIFIER)) {
            parser.advance(); // 消费 .
            Token identifier = parser.consume(TokenType.IDENTIFIER, "Expected identifier after '.'");
            className.append('.').append(identifier.getLexeme());
        }
        return className.toString();
    }

    /**
     * 解析括号内的参数列表
     * <p>
     * 假设左括号已被消费，解析 expr, expr, ... ) 格式
     *
     * @param parser 解析器实例
     * @return 参数表达式数组
     */
    public static ParseResult[] parseArgumentList(Parser parser) {
        List<ParseResult> args = new ArrayList<>();
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                args.add(parser.parseExpression());
            } while (parser.match(TokenType.COMMA));
        }
        parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
        return args.toArray(new ParseResult[0]);
    }
}
