package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.expression.ListExpression;
import org.tabooproject.fluxon.parser.expression.MapExpression;
import org.tabooproject.fluxon.parser.expression.literal.Literal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注解解析器
 * 解析 @name 或 @name(key = value, ...) 形式的注解
 * 
 * @author sky
 */
public class AnnotationParser {

    /**
     * 解析单个注解
     * @param parser 解析器
     * @return 注解对象
     */
    public static Annotation parse(Parser parser) {
        parser.consume(TokenType.AT, "Expected '@' for annotation");
        String name = parser.consume(TokenType.IDENTIFIER, "Expected annotation name").getLexeme();
        // 检查是否有属性
        if (parser.match(TokenType.LEFT_PAREN)) {
            Map<String, Object> attributes = parseAttributes(parser);
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after annotation attributes");
            return new Annotation(name, attributes);
        }
        parser.match(TokenType.SEMICOLON); // 可选的分号
        return new Annotation(name);
    }
    
    /**
     * 解析注解属性列表
     * @param parser 解析器
     * @return 属性映射
     */
    private static Map<String, Object> parseAttributes(Parser parser) {
        Map<String, Object> attributes = new HashMap<>();
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                Token keyToken = parser.consume(TokenType.IDENTIFIER, "Expected attribute name");
                String key = keyToken.getLexeme();
                parser.consume(TokenType.ASSIGN, "Expected '=' after attribute name");
                // 如果是列表
                if (parser.match(TokenType.LEFT_BRACKET)) {
                    ParseResult value = ListParser.parse(parser);
                    // 列表
                    if (value instanceof ListExpression) {
                        ListExpression listExpr = (ListExpression) value;
                        List<Object> literalValues = new ArrayList<>();
                        for (ParseResult element : listExpr.getElements()) {
                            if (element instanceof Literal) {
                                literalValues.add(((Literal) element).getSourceValue());
                            } else {
                                parser.error("Expected a literal value for list element in attribute '" + key + "'");
                            }
                        }
                        attributes.put(key, literalValues);
                    }
                    // 映射
                    else if (value instanceof MapExpression) {
                        MapExpression mapExpr = (MapExpression) value;
                        Map<Object, Object> literalValues = new HashMap<>();
                        for (MapExpression.MapEntry entry : mapExpr.getEntries()) {
                            ParseResult entryKey = entry.getKey();
                            ParseResult entryValue = entry.getValue();
                            if (entryKey instanceof Literal && entryValue instanceof Literal) {
                                literalValues.put(((Literal) entryKey).getSourceValue(), ((Literal) entryValue).getSourceValue());
                            } else {
                                parser.error("Expected literal values for map key and value in attribute '" + key + "'");
                            }
                        }
                        attributes.put(key, literalValues);
                    }
                } else {
                    ParseResult value = ExpressionParser.parsePrimary(parser);
                    if (value instanceof Literal) {
                        attributes.put(key, ((Literal) value).getSourceValue());
                    } else {
                        parser.error("Expected a literal value for attribute '" + key + "'");
                    }
                }
            } while (parser.match(TokenType.COMMA));
        }
        return attributes;
    }
}