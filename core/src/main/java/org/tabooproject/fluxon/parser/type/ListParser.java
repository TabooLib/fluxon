package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.expression.ListExpression;
import org.tabooproject.fluxon.parser.expression.MapExpression;

import java.util.ArrayList;
import java.util.List;

public class ListParser {

    /**
     * 尝试解析列表或字典字面量
     */
    public static ParseResult parse(Parser parser) {
        // 空列表 []
        if (parser.match(TokenType.RIGHT_BRACKET)) {
            boolean immutable = parser.match(TokenType.NOT);
            return new ListExpression(new ArrayList<>(), immutable);
        }
        // 空 Map [:]
        if (parser.match(TokenType.COLON)) {
            parser.consume(TokenType.RIGHT_BRACKET, "Expected ']' after empty map");
            boolean immutable = parser.match(TokenType.NOT);
            return new MapExpression(new ArrayList<>(), immutable);
        }
        // 检查是否是字典字面量
        boolean isDictionary = false;
        // 标记当前位置
        int mark = parser.mark();
        // 尝试解析一个表达式，然后检查后面是否跟着冒号
        ExpressionParser.parse(parser);
        if (parser.check(TokenType.COLON)) {
            isDictionary = true;
        }
        // 回滚
        parser.rollback(mark);
        return isDictionary ? parseMapLiteral(parser) : parseListLiteral(parser);
    }

    /**
     * 解析列表字面量
     *
     * @return 列表字面量解析结果
     */
    private static ParseResult parseListLiteral(Parser parser) {
        List<ParseResult> elements = new ArrayList<>();
        // 如果不是空列表
        if (!parser.check(TokenType.RIGHT_BRACKET)) {
            do {
                elements.add(ExpressionParser.parse(parser));
            } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RIGHT_BRACKET));
        }
        parser.consume(TokenType.RIGHT_BRACKET, "Expected ']' after list elements");
        boolean immutable = parser.match(TokenType.NOT);
        return new ListExpression(elements, immutable);
    }

    /**
     * 解析字典字面量
     *
     * @return 字典字面量解析结果
     */
    private static ParseResult parseMapLiteral(Parser parser) {
        List<MapExpression.MapEntry> entries = new ArrayList<>();
        // 如果不是空字典
        if (!parser.check(TokenType.RIGHT_BRACKET)) {
            do {
                // 解析键
                ParseResult key = ExpressionParser.parse(parser);
                // 解析冒号
                parser.consume(TokenType.COLON, "Expected ':' after map key");
                // 解析值
                ParseResult value = ExpressionParser.parse(parser);
                // 添加键值对
                entries.add(new MapExpression.MapEntry(key, value));
            } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RIGHT_BRACKET));
        }
        parser.consume(TokenType.RIGHT_BRACKET, "Expected ']' after map entries");
        boolean immutable = parser.match(TokenType.NOT);
        return new MapExpression(entries, immutable);
    }
}
