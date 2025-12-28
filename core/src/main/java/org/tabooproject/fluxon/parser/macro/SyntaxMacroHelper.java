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
     * 全限定名解析结果
     */
    public static class QualifiedNameResult {
        public final String name;
        public final boolean parenthesized;

        public QualifiedNameResult(String name, boolean parenthesized) {
            this.name = name;
            this.parenthesized = parenthesized;
        }
    }

    /**
     * 解析全限定类名（标识符序列，用 . 连接）
     * <p>
     * 支持两种格式：
     * <ul>
     *   <li>{@code java.util.ArrayList} - 普通格式</li>
     *   <li>{@code (java.util.ArrayList)} - 括号格式，用于消除歧义</li>
     * </ul>
     *
     * @param parser       解析器实例
     * @param errorMessage 首个标识符缺失时的错误消息
     * @return 解析结果，包含类名和是否使用括号的标志
     */
    public static QualifiedNameResult parseQualifiedName(Parser parser, String errorMessage) {
        // 括号模式：(ClassName)
        if (parser.check(TokenType.LEFT_PAREN)) {
            parser.advance(); // 消费 (
            String name = parseQualifiedNameInner(parser, errorMessage);
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after class name");
            return new QualifiedNameResult(name, true);
        }
        return new QualifiedNameResult(parseQualifiedNameInner(parser, errorMessage), false);
    }

    /**
     * 内部方法：解析标识符序列
     */
    private static String parseQualifiedNameInner(Parser parser, String errorMessage) {
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
