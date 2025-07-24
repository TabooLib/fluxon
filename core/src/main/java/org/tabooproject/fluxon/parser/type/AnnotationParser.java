package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.definition.Annotation;

import java.util.HashMap;
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
        
        Token nameToken = parser.consume(TokenType.IDENTIFIER, "Expected annotation name");
        String name = nameToken.getLexeme();
        
        // 检查是否有属性
        if (parser.match(TokenType.LEFT_PAREN)) {
            Map<String, ParseResult> attributes = parseAttributes(parser);
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after annotation attributes");
            return new Annotation(name, attributes);
        }
        
        return new Annotation(name);
    }
    
    /**
     * 解析注解属性列表
     * @param parser 解析器
     * @return 属性映射
     */
    private static Map<String, ParseResult> parseAttributes(Parser parser) {
        Map<String, ParseResult> attributes = new HashMap<>();
        
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                Token keyToken = parser.consume(TokenType.IDENTIFIER, "Expected attribute name");
                String key = keyToken.getLexeme();
                
                parser.consume(TokenType.ASSIGN, "Expected '=' after attribute name");
                
                ParseResult value = ExpressionParser.parse(parser);
                attributes.put(key, value);
            } while (parser.match(TokenType.COMMA));
        }
        
        return attributes;
    }
}